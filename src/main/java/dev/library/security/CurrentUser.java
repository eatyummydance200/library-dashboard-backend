package dev.library.security;

import dev.library.common.exception.AppException;
import dev.library.user.User;

public record CurrentUser(Long id, String username, String role, Long staffId) {
  public static CurrentUser from(User user) {
    return new CurrentUser(user.getId(), user.getUsername(), user.getRole().apiValue(),
        user.getStaff() == null ? null : user.getStaff().getId());
  }

  public boolean viewer() {
    return "viewer".equals(role);
  }

  public void requireAdmin() {
    if (!"admin".equals(role)) throw new AppException(403, "관리자 권한이 필요합니다.");
  }
}
