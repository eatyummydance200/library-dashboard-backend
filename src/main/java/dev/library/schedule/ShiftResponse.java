package dev.library.schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
public record ShiftResponse(Long id, @JsonProperty("staff_id") Long staffId, LocalDate day, int start, int end, String role) {
  public static ShiftResponse from(Shift shift) {
    return new ShiftResponse(shift.getId(), shift.getStaff().getId(), shift.getDay(), shift.getStart(), shift.getEnd(), shift.getRole().apiValue());
  }
}
