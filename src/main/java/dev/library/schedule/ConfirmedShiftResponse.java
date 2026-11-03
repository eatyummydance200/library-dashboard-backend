package dev.library.schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
public record ConfirmedShiftResponse(Long id, @JsonProperty("staff_id") Long staffId, int weekday, int start, int end, String role) {
  public static ConfirmedShiftResponse from(ConfirmedShift shift) {
    return new ConfirmedShiftResponse(shift.getId(), shift.getStaff().getId(), shift.getWeekday(), shift.getStart(), shift.getEnd(), shift.getRole().apiValue());
  }
}
