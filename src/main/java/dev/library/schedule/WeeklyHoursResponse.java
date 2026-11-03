package dev.library.schedule;
import com.fasterxml.jackson.annotation.JsonProperty;
public record WeeklyHoursResponse(@JsonProperty("staff_id") Long staffId, @JsonProperty("total_minutes") int totalMinutes, @JsonProperty("desk_minutes") int deskMinutes, int opens, int closes) {
  public static WeeklyHoursResponse from(WeeklyHourOverride hours) {
    return new WeeklyHoursResponse(hours.getStaff().getId(), hours.getTotalMinutes(), hours.getDeskMinutes(), hours.getOpens(), hours.getCloses());
  }
}
