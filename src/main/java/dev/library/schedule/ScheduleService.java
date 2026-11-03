package dev.library.schedule;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import dev.library.staff.Staff;
import dev.library.staff.StaffRepository;
import dev.library.staff.StaffResponse;
import dev.library.user.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ScheduleService {
  private final ShiftRepository shifts;
  private final ConfirmedShiftRepository confirmed;
  private final StaffHourOverrideRepository hours;
  private final StaffRepository staff;
  private final UserRepository users;
  private final WriteLock lock;

  public ScheduleService(ShiftRepository shifts, ConfirmedShiftRepository confirmed,
      StaffHourOverrideRepository hours, StaffRepository staff, UserRepository users, WriteLock lock) {
    this.shifts = shifts;
    this.confirmed = confirmed;
    this.hours = hours;
    this.staff = staff;
    this.users = users;
    this.lock = lock;
  }

  private LocalDate week(String value) {
    return monday(value == null ? today() : value);
  }

  private List<Shift> shifts(LocalDate start, LocalDate end) {
    return shifts.findByDayGreaterThanEqualAndDayLessThanOrderByDayAscStartAscIdAsc(start, end);
  }

  public Object schedule(String week) {
    LocalDate start = week(week);
    return Map.of("week", start, "staff", staff.findAllByOrderByIdAsc().stream().map(StaffResponse::from).toList(),
        "shifts", shifts(start, start.plusDays(7)).stream().map(ShiftResponse::from).toList());
  }

  public Object originalSchedule() {
    var entries = confirmed.findAllByOrderByWeekdayAscStartAscIdAsc();
    return Map.of("saved", !entries.isEmpty(),
        "staff", staff.findAllByOrderByIdAsc().stream().map(StaffResponse::from).toList(),
        "shifts", entries.stream().map(ConfirmedShiftResponse::from).toList());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveOriginalSchedule(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    if (!(body.get("shifts") instanceof List<?> raw) || raw.size() > 10000)
      throw new AppException(400, "확정 근무 목록을 확인해주세요.");
    lock.acquire();
    var entries = new ArrayList<ConfirmedShift>();
    for (Object value : raw) {
      if (!(value instanceof Map<?, ?> fields)) throw new AppException(400, "확정 근무 목록을 확인해주세요.");
      Object weekday = fields.get("weekday");
      if (!integer(weekday) || ((Number) weekday).longValue() < 1 || ((Number) weekday).longValue() > 5)
        fail("확정 근무는 월요일부터 금요일까지만 저장할 수 있습니다.");
      var values = new HashMap<String, Object>();
      for (String key : List.of("staff_id", "start", "end", "role")) values.put(key, fields.get(key));
      // Retired staff may stay in the confirmed table; copying skips them.
      var input = input(values, false);
      var shift = new ConfirmedShift();
      shift.setStaff(input.staff());
      shift.setWeekday(number(weekday));
      shift.setStart(input.start());
      shift.setEnd(input.end());
      shift.setRole(input.role());
      entries.add(shift);
    }
    var sorted = entries.stream().sorted(Comparator.comparingInt(ConfirmedShift::getWeekday)
        .thenComparing(shift -> shift.getStaff().getId()).thenComparingInt(ConfirmedShift::getStart)).toList();
    for (int i = 1; i < sorted.size(); i++) {
      var previous = sorted.get(i - 1);
      var current = sorted.get(i);
      if (previous.getWeekday() == current.getWeekday() && previous.getStaff().getId().equals(current.getStaff().getId())
          && previous.getEnd() > current.getStart()) throw new AppException(409, "같은 사람의 확정 근무 시간이 겹칩니다.");
    }
    confirmed.deleteAllInBatch();
    confirmed.saveAll(entries);
    return ok();
  }

  public Object weeklyHours() {
    return Map.of("overrides", hours.findAllByOrderByStaffIdAsc().stream().map(StaffHoursResponse::from).toList());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveWeeklyHours(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    int total = boundedInteger(body.get("total_minutes"), "총 근무 시간", 3900);
    int desk = boundedInteger(body.get("desk_minutes"), "데스크 시간", 3900);
    int opens = boundedInteger(body.get("opens"), "오픈 횟수", 5);
    int closes = boundedInteger(body.get("closes"), "마감 횟수", 5);
    if (total % 30 != 0 || desk % 30 != 0 || desk > total)
      fail("시간은 30분 단위이며 데스크 시간은 총 근무 시간을 초과할 수 없습니다.");
    lock.acquire();
    var person = staff.findById(id).orElseThrow(() -> new AppException(404, "근무자를 찾을 수 없습니다."));
    var value = hours.findByStaffId(id).orElseGet(StaffHourOverride::new);
    value.setStaff(person);
    value.setTotalMinutes(total);
    value.setDeskMinutes(desk);
    value.setOpens(opens);
    value.setCloses(closes);
    value.setUpdatedBy(users.getReferenceById(actor.id()));
    value.setUpdatedAt(Instant.now());
    hours.save(value);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object resetWeeklyHours(long id, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    hours.findByStaffId(id).ifPresent(hours::delete);
    return ok();
  }

  private ShiftInput input(Map<String, Object> body, boolean active) {
    if (!integer(body.get("staff_id")) || !integer(body.get("start")) || !integer(body.get("end")))
      fail("근무자와 시간을 확인해주세요.");
    long staffId = id(body.get("staff_id"), "근무자");
    long start = ((Number) body.get("start")).longValue(), end = ((Number) body.get("end")).longValue();
    Object role = body.get("role");
    if (start < 540 || end > 1320 || start >= end || start % 30 != 0 || end % 30 != 0
        || role == null || !List.of("desk", "general").contains(role))
      fail("근무 시간은 09:00~22:00 사이의 30분 단위입니다.");
    var person = staff.findById(staffId).filter(found -> !active || found.isActive())
        .orElseThrow(() -> new AppException(400, active ? "재직 중인 근무자를 선택해주세요." : "근무자를 찾을 수 없습니다."));
    return new ShiftInput(person, (int) start, (int) end, ShiftRole.valueOf(((String) role).toUpperCase(Locale.ROOT)));
  }

  private void apply(Shift shift, Map<String, Object> body, long exclude) {
    LocalDate day = day(body.get("day"));
    if (day.getDayOfWeek().getValue() > 5) fail("평일 날짜를 선택해주세요.");
    var input = input(body, true);
    if (shifts.overlaps(day, input.staff().getId(), input.start(), input.end(), exclude))
      throw new AppException(409, "같은 사람의 근무 시간이 겹칩니다.");
    shift.setStaff(input.staff());
    shift.setDay(day);
    shift.setStart(input.start());
    shift.setEnd(input.end());
    shift.setRole(input.role());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object addShift(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var shift = new Shift();
    apply(shift, body, 0);
    shifts.save(shift);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveShift(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var shift = shifts.findById(id).orElseThrow(() -> new AppException(404, "근무를 찾을 수 없습니다."));
    apply(shift, body, id);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object deleteShift(long id, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    shifts.findById(id).ifPresent(shifts::delete);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object copy(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    LocalDate target = monday(text(body.get("target"), "날짜", 10, true));
    boolean overwrite = Boolean.TRUE.equals(body.get("overwrite"));
    lock.acquire();
    var entries = confirmed.findAllByOrderByWeekdayAscStartAscIdAsc();
    if (entries.isEmpty()) throw new AppException(409, "확정된 근무 시간표를 먼저 저장해주세요.");
    var existing = shifts(target, target.plusDays(7));
    if (!existing.isEmpty()) {
      if (!overwrite) return Map.of("ok", false, "existing", existing.size());
      shifts.deleteAllInBatch(existing);
    }
    int count = 0, skipped = 0;
    for (var entry : entries) {
      if (!entry.getStaff().isActive()) {
        skipped++;
        continue;
      }
      var shift = new Shift();
      shift.setStaff(entry.getStaff());
      shift.setDay(target.plusDays(entry.getWeekday() - 1));
      shift.setStart(entry.getStart());
      shift.setEnd(entry.getEnd());
      shift.setRole(entry.getRole());
      shifts.save(shift);
      count++;
    }
    return Map.of("ok", true, "count", count, "skipped", skipped);
  }

  private List<ShiftResponse> active(List<Shift> entries, ZonedDateTime at) {
    if (at == null) return List.of();
    return entries.stream().filter(shift -> {
      var start = shift.getDay().atStartOfDay(KOREA);
      return !at.isBefore(start.plusMinutes(shift.getStart())) && at.isBefore(start.plusMinutes(shift.getEnd()));
    }).map(ShiftResponse::from).toList();
  }

  public Object status(String at) {
    ZonedDateTime moment;
    try {
      moment = at == null ? ZonedDateTime.now(KOREA) : ZonedDateTime.parse(at).withZoneSameInstant(KOREA);
    } catch (Exception e) {
      try {
        moment = LocalDateTime.parse(at).atZone(KOREA);
      } catch (Exception bad) {
        throw new AppException(400, "기준 시각을 확인해주세요.");
      }
    }
    var entries = shifts(moment.toLocalDate().minusDays(7), moment.toLocalDate().plusDays(8));
    var boundaries = new TreeSet<ZonedDateTime>();
    for (var shift : entries) {
      var start = shift.getDay().atStartOfDay(KOREA);
      boundaries.add(start.plusMinutes(shift.getStart()));
      boundaries.add(start.plusMinutes(shift.getEnd()));
    }
    var previous = boundaries.floor(moment);
    var next = boundaries.higher(moment);
    var previousAt = previous == null ? null : previous.minusNanos(1000);
    if (active(entries, moment).isEmpty() && previousAt != null && active(entries, previousAt).isEmpty()) {
      previousAt = null;
      for (var shift : entries) {
        var end = shift.getDay().atStartOfDay(KOREA).plusMinutes(shift.getEnd());
        if (!end.isAfter(moment) && (previousAt == null || end.isAfter(previousAt)))
          previousAt = end.minusNanos(1000);
      }
    }
    var out = new LinkedHashMap<String, Object>();
    out.put("at", moment.toOffsetDateTime().toString());
    out.put("previous", active(entries, previousAt));
    out.put("current", active(entries, moment));
    out.put("next", active(entries, next));
    out.put("previous_at", previous == null ? null : previous.toOffsetDateTime().toString());
    out.put("next_at", next == null ? null : next.toOffsetDateTime().toString());
    return out;
  }

  private static int boundedInteger(Object value, String label, int maximum) {
    if (!integer(value) || ((Number) value).longValue() < 0 || ((Number) value).longValue() > maximum)
      throw new AppException(400, label + "을(를) 확인해주세요.");
    return number(value);
  }

  private record ShiftInput(Staff staff, int start, int end, ShiftRole role) {}
}
