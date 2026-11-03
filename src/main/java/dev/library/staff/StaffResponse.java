package dev.library.staff;
public record StaffResponse(Long id, String name, String phone, int active) {
  public static StaffResponse from(Staff staff) {
    return new StaffResponse(staff.getId(), staff.getName(), staff.getPhone(), staff.isActive() ? 1 : 0);
  }
}
