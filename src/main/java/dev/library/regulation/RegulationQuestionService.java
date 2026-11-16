package dev.library.regulation;

import static dev.library.common.Validation.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.inquiry.SlackNotifier;
import dev.library.regulation.RegulationQuestionResponse.Extension;
import dev.library.regulation.RegulationQuestionResponse.Sources;
import dev.library.security.CurrentUser;
import dev.library.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/** No class-level transaction: the Claude call must not hold a DB connection or the write lock. */
@Service
public class RegulationQuestionService {
  static final int WARN_AFTER = 100;
  private static final Set<String> FILTERS = Set.of("flagged", "not_found", "all");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(KOREA);

  public record AskResponse(Long id, String outcome, String answer, List<String> sections, List<Extension> extensions,
      @JsonProperty("today_count") long todayCount, boolean warn) {}

  private final RegulationQuestionRepository repository;
  private final RegulationRepository regulations;
  private final UserRepository users;
  private final ChatbotClient chatbot;
  private final TransactionTemplate transaction;
  private final WriteLock lock;
  private final SlackNotifier slack;
  private final ObjectMapper mapper;

  RegulationQuestionService(RegulationQuestionRepository repository, RegulationRepository regulations,
      UserRepository users, ChatbotClient chatbot, TransactionTemplate transaction, WriteLock lock,
      SlackNotifier slack, ObjectMapper mapper) {
    this.repository = repository;
    this.regulations = regulations;
    this.users = users;
    this.chatbot = chatbot;
    this.transaction = transaction;
    this.lock = lock;
    this.slack = slack;
    this.mapper = mapper;
  }

  public AskResponse ask(Map<String, Object> body, CurrentUser actor) {
    String question = text(body.get("question"), "질문", 500, true);
    if (!chatbot.ready()) throw new AppException(503, "챗봇이 아직 설정되지 않았습니다.");
    String content = regulations.findById(1L).map(Regulation::getContent)
        .orElseThrow(() -> new AppException(503, "규정 문서가 아직 없습니다."));
    var reply = chatbot.ask(content, question);
    String sources = mapper.writeValueAsString(new Sources(reply.sections(), reply.extensions()));
    return transaction.execute(status -> {
      lock.acquire();
      var user = users.findById(actor.id()).orElseThrow(() -> new AppException(401, "로그인이 필요합니다."));
      var saved = repository.save(new RegulationQuestion(user, question, reply, sources, Instant.now()));
      long today = repository.countByUserIdAndCreatedAtGreaterThanEqual(actor.id(), startOfToday());
      return new AskResponse(saved.getId(), reply.outcome(), reply.answer(), reply.sections(), reply.extensions(),
          today, today > WARN_AFTER);
    });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RegulationQuestionResponse update(long id, Map<String, Object> body, CurrentUser actor) {
    Object flagged = body.get("flagged"), resolved = body.get("resolved");
    if (flagged == null && resolved == null) fail("입력값을 확인해주세요.");
    boolean flag = flagged != null && bool(flagged, "입력값을 확인해주세요.");
    boolean resolve = resolved != null && bool(resolved, "입력값을 확인해주세요.");
    String note = flag && body.get("note") != null ? text(body.get("note"), "메모", 500, false) : "";
    lock.acquire();
    var question = repository.findById(id).orElseThrow(() -> new AppException(404, "질문 기록을 찾을 수 없습니다."));
    if (resolved != null) actor.requireAdmin();
    if (flagged != null && !"admin".equals(actor.role()) && !question.getUser().getId().equals(actor.id()))
      throw new AppException(403, "본인 질문만 표시할 수 있습니다.");
    Instant now = Instant.now();
    if (flagged != null) {
      if (flag) {
        question.flag(now, note.isEmpty() ? null : note);
        slack.sendAfterCommit(message(question));
      } else {
        question.unflag();
      }
    }
    if (resolved != null) question.resolve(resolve ? now : null);
    return response(question);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> questions(String filter, CurrentUser actor) {
    actor.requireAdmin();
    String f = filter == null || filter.isBlank() ? "flagged" : filter;
    if (!FILTERS.contains(f)) fail("입력값을 확인해주세요.");
    LocalDate today = LocalDate.now(KOREA);
    Object[] month = repository.usageSince(today.withDayOfMonth(1).atStartOfDay(KOREA).toInstant()).get(0);
    Map<String, Object> summary = Map.of(
        "today_count", repository.countByCreatedAtGreaterThanEqual(startOfToday()),
        "month_count", ((Number) month[0]).longValue(),
        "month_cost_usd", cost(month));
    var items = repository.search(f, Limit.of(200)).stream().map(this::response).toList();
    return Map.of("summary", summary, "items", items);
  }

  /** Haiku 5.5 USD per million tokens: input 0.10, cache write 0.125, cache read 0.01, output 0.50. Reference only. */
  static double cost(Object[] usage) {
    double input = ((Number) usage[1]).doubleValue(), output = ((Number) usage[2]).doubleValue();
    double read = ((Number) usage[3]).doubleValue(), write = ((Number) usage[4]).doubleValue();
    double usd = (input * 0.10 + write * 0.125 + read * 0.01 + output * 0.50) / 1e6;
    return BigDecimal.valueOf(usd).setScale(4, RoundingMode.HALF_UP).doubleValue();
  }

  private static Instant startOfToday() {
    return LocalDate.now(KOREA).atStartOfDay(KOREA).toInstant();
  }

  private RegulationQuestionResponse response(RegulationQuestion question) {
    return RegulationQuestionResponse.from(question, mapper.readValue(question.getSources(), Sources.class));
  }

  /** Block Kit message. User text is escaped so it cannot turn into mentions or links. */
  private static Map<String, Object> message(RegulationQuestion q) {
    String head = ":warning: 챗봇 답변 문제 표시";
    var blocks = new java.util.ArrayList<Map<String, Object>>();
    blocks.add(section("*" + head + "*\n*질문*\n" + quote(q.getQuestion())));
    blocks.add(section("*답*\n" + quote(q.getAnswer())));
    if (q.getFlagNote() != null) blocks.add(section("*메모*\n" + quote(q.getFlagNote())));
    blocks.add(Map.of("type", "context", "elements", List.of(mrkdwn(
        SlackNotifier.escape(q.getUser().getUsername()) + " · " + TIME.format(q.getCreatedAt()) + " · #" + q.getId()))));
    blocks.add(Map.of("type", "divider"));
    return Map.of("text", head, "blocks", blocks);
  }

  private static String quote(String s) {
    return SlackNotifier.escape(s).lines().map(line -> ">" + line).collect(Collectors.joining("\n"));
  }

  private static Map<String, Object> section(String text) {
    return Map.of("type", "section", "text", mrkdwn(text));
  }

  private static Map<String, Object> mrkdwn(String text) {
    return Map.of("type", "mrkdwn", "text", text);
  }
}
