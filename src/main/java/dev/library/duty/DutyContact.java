package dev.library.duty;

import dev.library.teacher.Teacher;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "duty_contact")
public class DutyContact {
  @Id
  private Long id = 1L;

  @Column(name = "teacher", length = 20, nullable = false)
  private String teacher;

  @Column(name = "extension", length = 20, nullable = false)
  private String extension;

  @Column(name = "day")
  private LocalDate day;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id")
  private Teacher selectedTeacher;

  @Column(name = "phone", length = 20, nullable = false)
  private String phone;

  protected DutyContact() {}

  public Long getId() {
    return id;
  }

  public String getTeacher() {
    return teacher;
  }

  public void setTeacher(String teacher) {
    this.teacher = teacher;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public Teacher getSelectedTeacher() {
    return selectedTeacher;
  }

  public void setSelectedTeacher(Teacher selectedTeacher) {
    this.selectedTeacher = selectedTeacher;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
}
