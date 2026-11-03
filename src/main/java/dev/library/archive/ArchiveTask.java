package dev.library.archive;

import dev.library.user.User;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "archive_tasks", uniqueConstraints = {@UniqueConstraint(columnNames = {"assignment_id", "position"})})
public class ArchiveTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assignment_id", nullable = false)
  private ArchiveAssignment assignment;

  @Column(name = "position", nullable = false)
  private int position;

  @Column(name = "newspaper", length = 40, nullable = false)
  private String newspaper;

  @Column(name = "completed", nullable = false)
  private boolean completed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "completed_by")
  private User completedBy;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected ArchiveTask() {}

  public Long getId() {
    return id;
  }

  public ArchiveAssignment getAssignment() {
    return assignment;
  }

  public void setAssignment(ArchiveAssignment assignment) {
    this.assignment = assignment;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public String getNewspaper() {
    return newspaper;
  }

  public void setNewspaper(String newspaper) {
    this.newspaper = newspaper;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public User getCompletedBy() {
    return completedBy;
  }

  public void setCompletedBy(User completedBy) {
    this.completedBy = completedBy;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }
}
