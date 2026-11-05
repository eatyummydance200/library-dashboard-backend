package dev.library.cleaning;

import dev.library.staff.Staff;

import jakarta.persistence.*;

@Entity
@Table(name = "cleaning_assignments", uniqueConstraints = {@UniqueConstraint(columnNames = {"draw_id", "staff_id"})})
public class CleaningAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "draw_id", nullable = false)
  private CleaningDraw draw;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  protected CleaningAssignment() {}

  CleaningAssignment(CleaningDraw draw, Staff staff) {
    this.draw = draw;
    this.staff = staff;
  }

  public Long getId() {
    return id;
  }

  public CleaningDraw getDraw() {
    return draw;
  }

  public Staff getStaff() {
    return staff;
  }
}
