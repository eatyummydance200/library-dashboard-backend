package dev.library.schedule;

import dev.library.user.User;

import dev.library.staff.Staff;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "weekly_hour_overrides", uniqueConstraints = {@UniqueConstraint(columnNames = {"schedule_week_id", "staff_id"})})
public class WeeklyHourOverride {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_week_id", nullable = false)
  private OriginalScheduleWeek scheduleWeek;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "total_minutes", nullable = false)
  private int totalMinutes;

  @Column(name = "desk_minutes", nullable = false)
  private int deskMinutes;

  @Column(name = "opens", nullable = false)
  private int opens;

  @Column(name = "closes", nullable = false)
  private int closes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by", nullable = false)
  private User updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected WeeklyHourOverride() {}

  public Long getId() {
    return id;
  }

  public OriginalScheduleWeek getScheduleWeek() {
    return scheduleWeek;
  }

  public void setScheduleWeek(OriginalScheduleWeek scheduleWeek) {
    this.scheduleWeek = scheduleWeek;
  }

  public Staff getStaff() {
    return staff;
  }

  public void setStaff(Staff staff) {
    this.staff = staff;
  }

  public int getTotalMinutes() {
    return totalMinutes;
  }

  public void setTotalMinutes(int totalMinutes) {
    this.totalMinutes = totalMinutes;
  }

  public int getDeskMinutes() {
    return deskMinutes;
  }

  public void setDeskMinutes(int deskMinutes) {
    this.deskMinutes = deskMinutes;
  }

  public int getOpens() {
    return opens;
  }

  public void setOpens(int opens) {
    this.opens = opens;
  }

  public int getCloses() {
    return closes;
  }

  public void setCloses(int closes) {
    this.closes = closes;
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
