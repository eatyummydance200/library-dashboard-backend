package dev.library.schedule;

import dev.library.staff.Staff;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "original_shifts")
public class OriginalShift {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_week_id", nullable = false)
  private OriginalScheduleWeek scheduleWeek;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "day", nullable = false)
  private LocalDate day;

  @Column(name = "start_minute", nullable = false)
  private int start;

  @Column(name = "end_minute", nullable = false)
  private int end;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 16, nullable = false)
  private ShiftRole role;

  protected OriginalShift() {}

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

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public ShiftRole getRole() {
    return role;
  }

  public void setRole(ShiftRole role) {
    this.role = role;
  }
}
