package dev.library.issue;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.library.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class IssueService {
  private static final List<String> CATEGORIES = List.of("인계", "공지", "시설");
  private final IssueRepository repository;
  private final UserRepository users;
  private final WriteLock lock;

  public IssueService(IssueRepository repository, UserRepository users, WriteLock lock) {
    this.repository = repository;
    this.users = users;
    this.lock = lock;
  }

  public List<IssueResponse> issues(String day, String category, String week) {
    LocalDate selectedDay = day == null || day.isEmpty() ? null : day(day);
    if (category != null && !CATEGORIES.contains(category)) fail("이슈 구분을 확인해주세요.");
    LocalDate start = week == null || week.isEmpty() ? null : monday(week);
    if (start != null && start.plusDays(7).getYear() > 9999) fail("날짜 범위를 확인해주세요.");
    if ("".equals(day)) return List.of();
    Specification<Issue> filter = (root, query, builder) -> {
      var conditions = new ArrayList<Predicate>();
      if (selectedDay != null) {
        conditions.add(builder.lessThanOrEqualTo(root.get("day"), selectedDay));
        conditions.add(builder.greaterThanOrEqualTo(root.get("endDay"), selectedDay));
      }
      if (category != null) conditions.add(builder.equal(root.get("category"), category));
      if (start != null) {
        conditions.add(builder.lessThan(root.get("day"), start.plusDays(7)));
        conditions.add(builder.greaterThanOrEqualTo(root.get("endDay"), start));
      }
      return builder.and(conditions.toArray(Predicate[]::new));
    };
    return repository.findAll(filter, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
        .getContent().stream().map(IssueResponse::from).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object issue(Map<String, Object> body, CurrentUser actor) {
    LocalDate day = day(body.getOrDefault("day", today()));
    LocalDate endDay = endDay(body.get("end_day"), day);
    Object category = body.get("category");
    if (category == null || !CATEGORIES.contains(category)) fail("이슈 구분을 확인해주세요.");
    String title = text(body.get("title"), "제목", 80, true);
    String contents = text(body.get("body"), "내용", 1500, true);
    lock.acquire();
    var issue = new Issue();
    issue.setDay(day);
    issue.setEndDay(endDay == null ? day : endDay);
    issue.setCategory((String) category);
    issue.setTitle(title);
    issue.setBody(contents);
    issue.setAuthor(users.getReferenceById(actor.id()));
    issue.setCreatedAt(Instant.now());
    issue.setUpdatedAt(issue.getCreatedAt());
    repository.save(issue);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object toggleIssue(long id, Map<String, Object> body, CurrentUser actor) {
    boolean done = bool(body.get("done"), "이슈 상태를 확인해주세요.");
    lock.acquire();
    var issue = repository.findById(id).orElseThrow(() -> new AppException(404, "이슈를 찾을 수 없습니다."));
    issue.setDone(done);
    issue.setUpdatedAt(Instant.now());
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object delete(long id, CurrentUser actor) {
    lock.acquire();
    var issue = repository.findById(id).orElse(null);
    if (issue == null) return ok();
    if (!"admin".equals(actor.role()) && (issue.isDone() || !issue.getAuthor().getId().equals(actor.id())))
      throw new AppException(403, "진행 중인 본인 이슈만 삭제할 수 있습니다.");
    repository.delete(issue);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object update(long id, Map<String, Object> body, CurrentUser actor) {
    LocalDate day = day(body.get("day"));
    LocalDate endDay = endDay(body.get("end_day"), day);
    Object category = body.get("category");
    if (category == null || !CATEGORIES.contains(category)) fail("이슈 구분을 확인해주세요.");
    String title = text(body.get("title"), "제목", 80, true);
    String contents = text(body.get("body"), "내용", 1500, true);
    lock.acquire();
    var issue = repository.findById(id).orElseThrow(() -> new AppException(404, "이슈를 찾을 수 없습니다."));
    if (!"admin".equals(actor.role()) && !issue.getAuthor().getId().equals(actor.id()))
      throw new AppException(403, "작성자나 관리자만 수정할 수 있습니다.");
    if (issue.isDone()) throw new AppException(409, "완료된 이슈는 다시 연 뒤 수정해주세요.");
    if (endDay == null) endDay = issue.getEndDay().isBefore(day) ? day : issue.getEndDay();
    issue.setDay(day);
    issue.setEndDay(endDay);
    issue.setCategory((String) category);
    issue.setTitle(title);
    issue.setBody(contents);
    issue.setUpdatedAt(Instant.now());
    return ok();
  }

  private static LocalDate endDay(Object value, LocalDate day) {
    if (value == null) return null;
    LocalDate endDay = day(value);
    if (endDay.isBefore(day)) fail("종료일은 시작일과 같거나 이후로 선택해주세요.");
    return endDay;
  }
}
