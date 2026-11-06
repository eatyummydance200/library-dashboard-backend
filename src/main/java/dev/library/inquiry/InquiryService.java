package dev.library.inquiry;

import static dev.library.common.Validation.*;

import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import dev.library.user.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {
  // Slack 이모지 코드 + 라벨. 검증 목록도 겸한다.
  private static final Map<String, String> CATEGORIES = Map.of(
      "feature", ":bulb: 새 기능",
      "improvement", ":wrench: 기능 개선",
      "bug", ":bug: 오류",
      "other", ":speech_balloon: 기타");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(KOREA);
  private final InquiryRepository repository;
  private final UserRepository users;
  private final WriteLock lock;
  private final SlackNotifier slack;

  InquiryService(InquiryRepository repository, UserRepository users, WriteLock lock, SlackNotifier slack) {
    this.repository = repository;
    this.users = users;
    this.lock = lock;
    this.slack = slack;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object inquiry(Map<String, Object> body, CurrentUser actor) {
    // instanceof first: Map.of throws on containsKey(null).
    if (!(body.get("category") instanceof String category) || !CATEGORIES.containsKey(category))
      throw new AppException(400, "문의 유형을 확인해주세요.");
    String title = text(body.get("title"), "제목", 80, true);
    String contents = text(body.get("body"), "내용", 2000, true);
    lock.acquire();
    var author = users.findById(actor.id()).orElseThrow(() -> new AppException(401, "로그인이 필요합니다."));
    var inquiry = new Inquiry();
    inquiry.setCategory(category);
    inquiry.setTitle(title);
    inquiry.setBody(contents);
    inquiry.setAuthor(author);
    inquiry.setCreatedAt(Instant.now());
    repository.save(inquiry);
    // Built inside the transaction because author.staff is lazy.
    String name = author.getStaff() == null ? author.getUsername() : author.getStaff().getName();
    slack.sendAfterCommit(message(category, title, name, inquiry.getCreatedAt(), contents));
    return ok();
  }

  /** Block Kit 메시지. 사용자 입력(제목·작성자·본문)은 escape해서 멘션·링크로 해석되지 않게 한다. */
  static Map<String, Object> message(String category, String title, String author, Instant at, String body) {
    String head = CATEGORIES.get(category) + " · *" + SlackNotifier.escape(title) + "*";
    String quoted = SlackNotifier.escape(preview(body)).lines().map(line -> ">" + line).collect(Collectors.joining("\n"));
    return Map.of(
        "text", head,
        "blocks", List.of(
            Map.of("type", "section", "text", mrkdwn(head + "\n" + quoted)),
            Map.of("type", "context", "elements", List.of(mrkdwn(SlackNotifier.escape(author) + " · " + TIME.format(at)))),
            Map.of("type", "divider")));
  }

  private static Map<String, Object> mrkdwn(String text) {
    return Map.of("type", "mrkdwn", "text", text);
  }

  /** First 500 code points, then "…". */
  static String preview(String s) {
    return s.codePointCount(0, s.length()) > 500 ? s.substring(0, s.offsetByCodePoints(0, 500)) + "…" : s;
  }
}
