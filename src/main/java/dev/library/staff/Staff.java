package dev.library.staff;

import jakarta.persistence.*;

@Entity
@Table(name = "staff", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class Staff {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", length = 20, nullable = false)
  private String name;

  @Column(name = "phone", length = 20, nullable = false)
  private String phone;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected Staff() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
