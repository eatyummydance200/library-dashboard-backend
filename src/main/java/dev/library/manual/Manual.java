package dev.library.manual;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manual")
public class Manual {
  @Id
  private Long id = 1L;

  @Column(name = "content", length = ManualService.MAX_CONTENT, nullable = false)
  private String content;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by", length = 32, nullable = false)
  private String updatedBy;

  protected Manual() {}

  Manual(String content, Instant updatedAt, String updatedBy) {
    update(content, updatedAt, updatedBy);
  }

  void update(String content, Instant updatedAt, String updatedBy) {
    this.content = content;
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public String getContent() {
    return content;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }
}
