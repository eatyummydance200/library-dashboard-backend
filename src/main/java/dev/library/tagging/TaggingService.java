package dev.library.tagging;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.library.staff.StaffRepository;
import dev.library.user.UserRepository;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class TaggingService {
  private final MonthlyTagRepository repository;
  private final StaffRepository staff;
  private final UserRepository users;
  private final WriteLock lock;

  public TaggingService(MonthlyTagRepository repository, StaffRepository staff, UserRepository users, WriteLock lock) {
    this.repository = repository;
    this.staff = staff;
    this.users = users;
    this.lock = lock;
  }

  public Object tagging(String year) {
    if (year == null) year = String.valueOf(LocalDate.now(KOREA).getYear());
    if (!year.matches("[0-9]{4}")) fail("연도를 확인해주세요.");
    int value = Integer.parseInt(year);
    period(value, 1);
    var tags = repository.findByYearOrderByStaffIdAscMonthAsc(value);
    var retained = tags.stream().map(tag -> tag.getStaff().getId()).collect(Collectors.toSet());
    var people = staff.findAllByOrderByIdAsc().stream()
        .filter(person -> person.isActive() || retained.contains(person.getId()))
        .map(person -> new Participant(person.getId(), person.getName(), person.isActive() ? 1 : 0)).toList();
    return Map.of("year", value, "staff", people, "tags", tags.stream()
        .map(tag -> new Tag(tag.getStaff().getId(), tag.getMonth(), tag.isCompleted() ? 1 : 0)).toList());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveTag(long id, int year, int month, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    period(year, month);
    boolean done = bool(body.get("completed"), "태깅 완료 여부를 확인해주세요.");
    lock.acquire();
    var person = staff.findById(id).orElseThrow(() -> new AppException(404, "근무자를 찾을 수 없습니다."));
    var tag = repository.findByStaffIdAndYearAndMonth(id, year, month).orElseGet(MonthlyTag::new);
    tag.setStaff(person);
    tag.setYear(year);
    tag.setMonth(month);
    tag.setCompleted(done);
    tag.setUpdatedBy(users.getReferenceById(actor.id()));
    tag.setUpdatedAt(Instant.now());
    repository.save(tag);
    return ok();
  }

  public record Participant(Long id, String name, int active) {}
  public record Tag(@JsonProperty("staff_id") Long staffId, int month, int completed) {}
}
