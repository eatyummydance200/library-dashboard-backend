package dev.library.archive;

import dev.library.staff.Staff;

import jakarta.persistence.*;

@Entity
@Table(name = "archive_assignments", uniqueConstraints = {@UniqueConstraint(columnNames = {"draw_id", "group_number"}), @UniqueConstraint(columnNames = {"draw_id", "staff_id"})})
public class ArchiveAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "draw_id", nullable = false)
  private ArchiveDraw draw;

  @Column(name = "group_number", nullable = false)
  private int groupNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "staff_id", nullable = false)
  private Staff staff;

  @Column(name = "staff_name", length = 20, nullable = false)
  private String staffName;

  protected ArchiveAssignment() {}

  public Long getId() {
    return id;
  }

  public ArchiveDraw getDraw() {
    return draw;
  }

  public void setDraw(ArchiveDraw draw) {
    this.draw = draw;
  }

  public int getGroupNumber() {
    return groupNumber;
  }

  public void setGroupNumber(int groupNumber) {
    this.groupNumber = groupNumber;
  }

  public Staff getStaff() {
    return staff;
  }

  public void setStaff(Staff staff) {
    this.staff = staff;
  }

  public String getStaffName() {
    return staffName;
  }

  public void setStaffName(String staffName) {
    this.staffName = staffName;
  }
}
