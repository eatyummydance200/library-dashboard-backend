package dev.library.teacher;

import jakarta.persistence.*;

@Entity
@Table(name = "teachers", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class Teacher {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", length = 20, nullable = false)
  private String name;

  @Column(name = "extension", length = 20, nullable = false)
  private String extension;

  @Column(name = "phone", length = 20, nullable = false)
  private String phone;

  @Column(name = "position", length = 40, nullable = false)
  private String position;

  @Column(name = "location", length = 80, nullable = false)
  private String location;

  protected Teacher() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }
}
