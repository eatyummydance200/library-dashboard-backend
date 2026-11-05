package dev.library.cleaning;

import static dev.library.common.Validation.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import dev.library.staff.Staff;
import dev.library.staff.StaffRepository;
import dev.library.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CleaningService {
  private static final String MONTH_ERROR = "청소 기준 월을 확인해주세요.";
  private final CleaningDrawRepository draws;
  private final CleaningAssignmentRepository assignments;
  private final StaffRepository staff;
  private final UserRepository users;
  private final WriteLock lock;

  public CleaningService(CleaningDrawRepository draws, CleaningAssignmentRepository assignments,
      StaffRepository staff, UserRepository users, WriteLock lock) {
    this.draws = draws;
    this.assignments = assignments;
    this.staff = staff;
    this.users = users;
    this.lock = lock;
  }

  public List<CleaningResponse> list() {
    var byDraw = assignments.findAllByOrderByStaffIdAsc().stream()
        .collect(Collectors.groupingBy(a -> a.getDraw().getId()));
    return draws.findAllByOrderByMonthDesc().stream()
        .map(draw -> CleaningResponse.from(draw, byDraw.getOrDefault(draw.getId(), List.of()))).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public CleaningResponse draw(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    LocalDate month = month(body.get("month"), MONTH_ERROR);
    Object count = body.get("count");
    boolean replace = Boolean.TRUE.equals(body.get("replace"));
    lock.acquire();
    var people = new ArrayList<>(staff.findByActiveTrueOrderByIdAsc());
    if (!integer(count) || number(count) < 1 || number(count) > people.size())
      fail("청소 담당 인원을 확인해주세요. 재직 근무자는 " + people.size() + "명입니다.");
    if (draws.findByMonth(month).isPresent() && !replace)
      throw new AppException(409, "이미 추첨한 월입니다.");
    Collections.shuffle(people, new SecureRandom());
    return save(month, people.subList(0, number(count)), actor);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public CleaningResponse assign(String monthText, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    LocalDate month = month(monthText, MONTH_ERROR);
    if (!(body.get("staff_ids") instanceof List<?> entries) || entries.isEmpty())
      throw new AppException(400, "청소 담당자를 선택해주세요.");
    var ids = entries.stream().map(entry -> id(entry, "청소 담당자")).toList();
    if (new HashSet<>(ids).size() != ids.size()) fail("청소 담당자를 중복 없이 선택해주세요.");
    lock.acquire();
    var people = new ArrayList<Staff>();
    for (long id : ids) {
      var person = staff.findById(id).filter(Staff::isActive)
          .orElseThrow(() -> new AppException(400, "재직 중인 근무자를 선택해주세요."));
      people.add(person);
    }
    return save(month, people, actor);
  }

  // Monthly result only: replaces the month's assignments and stamps the last actor/time.
  private CleaningResponse save(LocalDate month, List<Staff> people, CurrentUser actor) {
    var draw = draws.findByMonth(month).orElse(null);
    if (draw == null) {
      draw = new CleaningDraw();
      draw.setMonth(month);
    } else {
      assignments.deleteByDrawId(draw.getId());
    }
    draw.setCreatedBy(users.getReferenceById(actor.id()));
    draw.setCreatedAt(Instant.now());
    draw = draws.saveAndFlush(draw);
    for (Staff person : people) assignments.save(new CleaningAssignment(draw, person));
    assignments.flush();
    var selected = people.stream().sorted(Comparator.comparing(Staff::getId))
        .map(person -> new StaffName(person.getId(), person.getName())).toList();
    return new CleaningResponse(YearMonth.from(month).toString(), actor.id(), actor.username(), draw.getCreatedAt(), selected);
  }

  public record StaffName(Long id, String name) {}
  public record CleaningResponse(String month, @JsonProperty("created_by") Long createdBy, String username,
      @JsonProperty("created_at") Instant createdAt, List<StaffName> staff) {
    static CleaningResponse from(CleaningDraw draw, List<CleaningAssignment> selected) {
      return new CleaningResponse(YearMonth.from(draw.getMonth()).toString(), draw.getCreatedBy().getId(),
          draw.getCreatedBy().getUsername(), draw.getCreatedAt(),
          selected.stream().map(a -> new StaffName(a.getStaff().getId(), a.getStaff().getName())).toList());
    }
  }
}
