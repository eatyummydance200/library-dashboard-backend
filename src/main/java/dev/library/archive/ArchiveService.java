package dev.library.archive;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import dev.library.staff.Staff;
import dev.library.staff.StaffRepository;
import dev.library.user.UserRepository;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ArchiveService {
  private static final List<List<String>> GROUPS = List.of(
      List.of("중앙", "코헤", "FT"), List.of("국민", "경기", "경인"), List.of("동아", "아사히", "인민"),
      List.of("조선", "코탐"), List.of("매일경제", "전자", "요미"), List.of("한국경제", "인천", "경향"),
      List.of("한국일보", "한겨레", "뉴탐"));
  private final ArchiveDrawRepository draws;
  private final ArchiveAssignmentRepository assignments;
  private final ArchiveTaskRepository tasks;
  private final StaffRepository staff;
  private final UserRepository users;
  private final ArchiveReassignmentRepository reassignments;
  private final WriteLock lock;

  public ArchiveService(ArchiveDrawRepository draws, ArchiveAssignmentRepository assignments,
      ArchiveTaskRepository tasks, StaffRepository staff, UserRepository users,
      ArchiveReassignmentRepository reassignments, WriteLock lock) {
    this.draws = draws;
    this.assignments = assignments;
    this.tasks = tasks;
    this.staff = staff;
    this.users = users;
    this.reassignments = reassignments;
    this.lock = lock;
  }

  public ArchiveResponse archive(String month) {
    LocalDate date = month(month == null ? today().substring(0, 7) : month);
    var draw = draws.findByMonth(date).orElse(null);
    var selected = draw == null ? List.<ArchiveAssignment>of() : assignments.findByDrawIdOrderByGroupNumberAsc(draw.getId());
    var items = draw == null ? List.<ArchiveTask>of() : tasks.findByAssignmentDrawIdOrderByPositionAsc(draw.getId());
    var byAssignment = items.stream().collect(Collectors.groupingBy(task -> task.getAssignment().getId()));
    var result = selected.stream().map(assignment -> new AssignmentResponse(
        assignment.getId(), assignment.getDraw().getId(), assignment.getGroupNumber(),
        assignment.getStaff().getId(), assignment.getStaffName(),
        byAssignment.getOrDefault(assignment.getId(), List.of()).stream().map(TaskResponse::from).toList())).toList();
    return new ArchiveResponse(YearMonth.from(date).toString(), draw == null ? null : DrawResponse.from(draw),
        result, GROUPS, staff.findByActiveTrueOrderByIdAsc().stream().map(person -> new Participant(person.getId(), person.getName())).toList());
  }

  private ArchiveDraw createDraw(LocalDate month, CurrentUser actor) {
    var draw = new ArchiveDraw();
    draw.setMonth(month);
    draw.setCreatedBy(users.getReferenceById(actor.id()));
    draw.setCreatedAt(Instant.now());
    return draws.saveAndFlush(draw);
  }

  private void createAssignments(ArchiveDraw draw, List<Staff> people) {
    for (int i = 0; i < GROUPS.size(); i++) {
      var assignment = new ArchiveAssignment();
      assignment.setDraw(draw);
      assignment.setGroupNumber(i + 1);
      assignment.setStaff(people.get(i));
      assignment.setStaffName(people.get(i).getName());
      assignments.save(assignment);
      for (int j = 0; j < GROUPS.get(i).size(); j++) {
        var task = new ArchiveTask();
        task.setAssignment(assignment);
        task.setPosition(j);
        task.setNewspaper(GROUPS.get(i).get(j));
        tasks.save(task);
      }
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object draw(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    LocalDate month = month(body.get("month"));
    lock.acquire();
    if (draws.findByMonth(month).isPresent()) throw new AppException(409, "이미 추첨한 월입니다. 저장된 결과를 확인해주세요.");
    var people = new ArrayList<>(staff.findByActiveTrueOrderByIdAsc());
    if (people.size() != 7) fail("7개 신문 묶음의 추첨에는 재직 근무자 7명이 필요합니다. 인적관리를 확인해주세요.");
    Collections.shuffle(people, new java.security.SecureRandom());
    createAssignments(createDraw(month, actor), people);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object manual(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    LocalDate month = month(body.get("month"));
    var ids = ids(body.get("staff_ids"), true);
    var expected = body.get("expected") == null ? null : ids(body.get("expected"), false);
    lock.acquire();
    var draw = draws.findByMonth(month).orElse(null);
    var existing = draw == null ? List.<ArchiveAssignment>of() : assignments.findByDrawIdOrderByGroupNumberAsc(draw.getId());
    var current = existing.isEmpty() ? null : existing.stream().map(assignment -> assignment.getStaff().getId()).toList();
    if (!Objects.equals(expected, current))
      throw new AppException(409, "다른 관리자가 담당 배정을 변경했습니다. 새로고침 후 다시 확인해주세요.");
    if (draw != null && existing.size() != 7) throw new AppException(409, "저장된 담당 배정을 확인해주세요.");
    var people = new ArrayList<Staff>();
    var names = new ArrayList<String>();
    for (int i = 0; i < ids.size(); i++) {
      var person = staff.findById(ids.get(i)).orElse(null);
      boolean unchanged = !existing.isEmpty() && existing.get(i).getStaff().getId().equals(ids.get(i));
      if (person == null || !person.isActive() && !unchanged)
        fail("재직 중인 근무자를 선택해주세요. 기존 비활성 담당자는 그대로 유지할 수 있습니다.");
      people.add(person);
      names.add(unchanged ? existing.get(i).getStaffName() : person.getName());
    }
    if (draw == null) {
      createAssignments(createDraw(month, actor), people);
    } else if (!ids.equals(current)) {
      reassignments.replace(existing, tasks.findByAssignmentDrawIdOrderByPositionAsc(draw.getId()), people, names);
    }
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object complete(long id, Map<String, Object> body, CurrentUser actor) {
    boolean done = bool(body.get("completed"), "완료 여부를 확인해주세요.");
    lock.acquire();
    var task = tasks.findById(id).orElseThrow(() -> new AppException(404, "신문사 과월호 항목을 찾을 수 없습니다."));
    if (!"admin".equals(actor.role()) && !Objects.equals(task.getAssignment().getStaff().getId(), actor.staffId()))
      throw new AppException(403, "본인이 담당한 신문사만 완료 체크할 수 있습니다.");
    if (task.isCompleted() != done) {
      task.setCompleted(done);
      task.setCompletedBy(done ? users.getReferenceById(actor.id()) : null);
      task.setCompletedAt(done ? Instant.now() : null);
    }
    return ok();
  }

  private static LocalDate month(Object value) {
    return Validation.month(value, "과월호 기준 월을 확인해주세요.");
  }

  private static List<Long> ids(Object value, boolean distinct) {
    if (!(value instanceof List<?> entries) || entries.size() != 7)
      throw new AppException(400, "7개 묶음의 담당자를 선택해주세요.");
    var ids = ((List<?>) value).stream().map(entry -> id(entry, "담당자")).toList();
    if (distinct && new HashSet<>(ids).size() != 7) fail("담당자를 중복 없이 선택해주세요.");
    return ids;
  }

  public record Participant(Long id, String name) {}
  public record ArchiveResponse(String month, DrawResponse draw, List<AssignmentResponse> assignments,
      List<List<String>> groups, List<Participant> participants) {}
  public record DrawResponse(Long id, String month, @JsonProperty("created_by") Long createdBy,
      @JsonProperty("created_at") Instant createdAt, @JsonProperty("created_by_name") String createdByName) {
    static DrawResponse from(ArchiveDraw draw) {
      return new DrawResponse(draw.getId(), YearMonth.from(draw.getMonth()).toString(),
          draw.getCreatedBy().getId(), draw.getCreatedAt(), draw.getCreatedBy().getUsername());
    }
  }
  public record AssignmentResponse(Long id, @JsonProperty("draw_id") Long drawId,
      @JsonProperty("group_number") int groupNumber, @JsonProperty("staff_id") Long staffId,
      @JsonProperty("staff_name") String staffName, List<TaskResponse> tasks) {}
  public record TaskResponse(Long id, @JsonProperty("assignment_id") Long assignmentId,
      int position, String newspaper, int completed, @JsonProperty("completed_by") Long completedBy,
      @JsonProperty("completed_at") Instant completedAt, @JsonProperty("completed_by_name") String completedByName) {
    static TaskResponse from(ArchiveTask task) {
      var user = task.getCompletedBy();
      return new TaskResponse(task.getId(), task.getAssignment().getId(), task.getPosition(), task.getNewspaper(),
          task.isCompleted() ? 1 : 0, user == null ? null : user.getId(), task.getCompletedAt(), user == null ? null : user.getUsername());
    }
  }
}
