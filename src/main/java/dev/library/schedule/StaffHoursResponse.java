package dev.library.schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
public record StaffHoursResponse(@JsonProperty("staff_id") Long staffId, @JsonProperty("total_minutes") int totalMinutes, @JsonProperty("desk_minutes") int deskMinutes, int opens, int closes) {
  public static StaffHoursResponse from(StaffHourOverride hours) {
    return new StaffHoursResponse(hours.getStaff().getId(), hours.getTotalMinutes(), hours.getDeskMinutes(), hours.getOpens(), hours.getCloses());
  }
}
