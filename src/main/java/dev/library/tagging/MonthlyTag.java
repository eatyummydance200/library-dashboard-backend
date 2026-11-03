package dev.library.tagging;

import dev.library.user.User;

import dev.library.staff.Staff;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "monthly_tags", uniqueConstraints = {@UniqueConstraint(columnNames = {"staff_id", "tag_year", "tag_month"})})
public class MonthlyTag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "tag_year", nullable = false)
  private int year;

  @Column(name = "tag_month", nullable = false)
  private int month;

  @Column(name = "completed", nullable = false)
  private boolean completed;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by", nullable = false)
  private User updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MonthlyTag() {}

  public Long getId() {
    return id;
  }

  public Staff getStaff() {
    return staff;
  }

  public void setStaff(Staff staff) {
    this.staff = staff;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
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
