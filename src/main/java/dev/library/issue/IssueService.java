package dev.library.issue;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.schedule.ScheduleService;
import dev.library.schedule.ShiftRepository;
import dev.library.schedule.ShiftResponse;
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
  private static final List<String> CATEGORIES = List.of("인계", "공지", "시설", "근무 변경");
  private final IssueRepository repository;
  private final UserRepository users;
  private final ShiftRepository shifts;
  private final ScheduleService schedule;
  private final WriteLock lock;

  public IssueService(IssueRepository repository, UserRepository users, ShiftRepository shifts,
      ScheduleService schedule, WriteLock lock) {
    this.repository = repository;
    this.users = users;
    this.shifts = shifts;
    this.schedule = schedule;
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
      if (selectedDay != null) conditions.add(builder.equal(root.get("day"), selectedDay));
      if (category != null) conditions.add(builder.equal(root.get("category"), category));
      if (start != null) {
        conditions.add(builder.greaterThanOrEqualTo(root.get("day"), start));
        conditions.add(builder.lessThan(root.get("day"), start.plusDays(7)));
      }
      return builder.and(conditions.toArray(Predicate[]::new));
    };
    return repository.findAll(filter, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
        .getContent().stream().map(IssueResponse::from).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object issue(Map<String, Object> body, CurrentUser actor) {
    LocalDate day = day(body.getOrDefault("day", today()));
    Object category = body.get("category");
    if (category == null || !CATEGORIES.contains(category)) fail("이슈 구분을 확인해주세요.");
    String title = text(body.get("title"), "제목", 80, true);
    String contents = text(body.get("body"), "내용", 1500, true);
    lock.acquire();
    var issue = new Issue();
    if (body.get("shift_id") != null) {
      if (!"근무 변경".equals(category)) fail("근무 변경 요청만 근무를 지정할 수 있습니다.");
      var shift = shifts.findById(id(body.get("shift_id"), "근무"))
          .orElseThrow(() -> new AppException(404, "근무를 찾을 수 없습니다."));
      if (!"admin".equals(actor.role()) && !shift.getStaff().getId().equals(actor.staffId()))
        throw new AppException(403, "본인 근무만 변경 요청할 수 있습니다.");
      if (body.get("expected_shift") instanceof Map<?, ?> expected) {
        var current = ShiftResponse.from(shift);
        var now = List.of(current.staffId(), current.day(), current.start(), current.end(), current.role());
        var then = Arrays.asList(expected.get("staff_id"), expected.get("day"), expected.get("start"), expected.get("end"), expected.get("role"));
        if (!now.stream().map(String::valueOf).toList().equals(then.stream().map(String::valueOf).toList()))
          throw new AppException(409, "근무가 변경되었습니다. 근무표를 새로고침해주세요.");
      }
      issue.setShift(shift);
      day = shift.getDay();
    }
    issue.setDay(day);
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
    if (issue.getShiftStart() != null) actor.requireAdmin();
    issue.setDone(done);
    issue.setUpdatedAt(Instant.now());
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object update(long id, Map<String, Object> body, CurrentUser actor) {
    String title = text(body.get("title"), "제목", 80, true);
    String contents = text(body.get("body"), "내용", 1500, true);
    lock.acquire();
    var issue = repository.findById(id).orElseThrow(() -> new AppException(404, "이슈를 찾을 수 없습니다."));
    if (!"admin".equals(actor.role()) && !issue.getAuthor().getId().equals(actor.id()))
      throw new AppException(403, "작성자나 관리자만 수정할 수 있습니다.");
    if (issue.isDone()) throw new AppException(409, "완료된 이슈는 다시 연 뒤 수정해주세요.");
    // Shift change requests keep their day and category; only the text is editable.
    if (issue.getShiftStart() == null) {
      Object category = body.get("category");
      if (category == null || !CATEGORIES.contains(category) || "근무 변경".equals(category))
        fail("이슈 구분을 확인해주세요.");
      issue.setDay(day(body.get("day")));
      issue.setCategory((String) category);
    }
    issue.setTitle(title);
    issue.setBody(contents);
    issue.setUpdatedAt(Instant.now());
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object apply(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var issue = repository.findById(id).orElseThrow(() -> new AppException(404, "이슈를 찾을 수 없습니다."));
    if (issue.isDone()) throw new AppException(409, "이미 완료된 요청입니다.");
    if (issue.getShift() == null) throw new AppException(409, "요청한 근무가 삭제되었습니다.");
    schedule.saveShift(issue.getShift().getId(), body, actor);
    issue.setDone(true);
    issue.setUpdatedAt(Instant.now());
    return ok();
  }
}
