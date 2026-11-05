package dev.library.cleaning;

import dev.library.user.User;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cleaning_draws", uniqueConstraints = {@UniqueConstraint(columnNames = {"month"})})
public class CleaningDraw {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "month", nullable = false)
  private LocalDate month;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CleaningDraw() {}

  public Long getId() {
    return id;
  }

  public LocalDate getMonth() {
    return month;
  }

  public void setMonth(LocalDate month) {
    this.month = month;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
