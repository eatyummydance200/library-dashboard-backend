package dev.library.schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
public record OriginalShiftResponse(Long id, LocalDate week, @JsonProperty("staff_id") Long staffId, LocalDate day, int start, int end, String role) {
  public static OriginalShiftResponse from(OriginalShift shift) {
    return new OriginalShiftResponse(shift.getId(), shift.getScheduleWeek().getWeek(), shift.getStaff().getId(), shift.getDay(), shift.getStart(), shift.getEnd(), shift.getRole().apiValue());
  }
}
