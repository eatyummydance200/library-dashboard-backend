package dev.library.user;
import com.fasterxml.jackson.annotation.JsonProperty;
public record UserResponse(Long id, String username, String role, int active, @JsonProperty("staff_id") Long staffId) {
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getUsername(), user.getRole().apiValue(), user.isActive() ? 1 : 0, user.getStaff() == null ? null : user.getStaff().getId());
  }
}
