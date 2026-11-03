package dev.library.schedule;

import dev.library.staff.Staff;

import jakarta.persistence.*;

@Entity
@Table(name = "confirmed_shifts")
public class ConfirmedShift {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "weekday", nullable = false)
  private int weekday;

  @Column(name = "start_minute", nullable = false)
  private int start;

  @Column(name = "end_minute", nullable = false)
  private int end;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 16, nullable = false)
  private ShiftRole role;

  protected ConfirmedShift() {}

  public Long getId() {
    return id;
  }

  public Staff getStaff() {
    return staff;
  }

  public void setStaff(Staff staff) {
    this.staff = staff;
  }

  public int getWeekday() {
    return weekday;
  }

  public void setWeekday(int weekday) {
    this.weekday = weekday;
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
