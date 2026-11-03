package dev.library.schedule;

import dev.library.user.User;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "original_schedule_weeks", uniqueConstraints = {@UniqueConstraint(columnNames = {"week"})})
public class OriginalScheduleWeek {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "week", nullable = false)
  private LocalDate week;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by", nullable = false)
  private User updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OriginalScheduleWeek() {}

  public Long getId() {
    return id;
  }

  public LocalDate getWeek() {
    return week;
  }

  public void setWeek(LocalDate week) {
    this.week = week;
  }

  public User getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(User updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
