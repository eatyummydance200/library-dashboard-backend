package dev.library.regulation;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "regulations")
public class Regulation {
  @Id
  private Long id = 1L;

  @Column(name = "content", length = RegulationService.MAX_CONTENT, nullable = false)
  private String content;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by", length = 32, nullable = false)
  private String updatedBy;

  protected Regulation() {}

  Regulation(String content, Instant updatedAt, String updatedBy) {
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
