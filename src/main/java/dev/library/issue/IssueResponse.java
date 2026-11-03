package dev.library.issue;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.Instant;
public record IssueResponse(Long id, LocalDate day, String category, String title, String body, @JsonProperty("author_id") Long authorId, int done, @JsonProperty("created_at") Instant createdAt, @JsonProperty("updated_at") Instant updatedAt, String author,
    @JsonProperty("shift_id") Long shiftId, @JsonProperty("shift_staff_id") Long shiftStaffId, @JsonProperty("shift_name") String shiftName,
    @JsonProperty("shift_start") Integer shiftStart, @JsonProperty("shift_end") Integer shiftEnd, @JsonProperty("shift_role") String shiftRole, @JsonProperty("current_day") LocalDate currentDay) {
  public static IssueResponse from(Issue issue) {
    var shift = issue.getShift();
    var staff = issue.getShiftStaff();
    return new IssueResponse(issue.getId(), issue.getDay(), issue.getCategory(), issue.getTitle(), issue.getBody(), issue.getAuthor().getId(), issue.isDone() ? 1 : 0, issue.getCreatedAt(), issue.getUpdatedAt(), issue.getAuthor().getUsername(),
        shift == null ? null : shift.getId(), staff == null ? null : staff.getId(), staff == null ? null : staff.getName(),
        issue.getShiftStart(), issue.getShiftEnd(), issue.getShiftRole() == null ? null : issue.getShiftRole().apiValue(), shift == null ? null : shift.getDay());
  }
}
