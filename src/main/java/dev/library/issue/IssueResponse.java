package dev.library.issue;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.Instant;
public record IssueResponse(Long id, LocalDate day, @JsonProperty("end_day") LocalDate endDay, String category, String title, String body, @JsonProperty("author_id") Long authorId, int done, @JsonProperty("created_at") Instant createdAt, @JsonProperty("updated_at") Instant updatedAt, String author) {
  public static IssueResponse from(Issue issue) {
    return new IssueResponse(issue.getId(), issue.getDay(), issue.getEndDay(), issue.getCategory(), issue.getTitle(), issue.getBody(), issue.getAuthor().getId(), issue.isDone() ? 1 : 0, issue.getCreatedAt(), issue.getUpdatedAt(), issue.getAuthor().getUsername());
  }
}
