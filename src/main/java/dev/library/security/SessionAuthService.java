package dev.library.security;

import dev.library.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionAuthService {
  private final UserRepository repository;

  public SessionAuthService(UserRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public CurrentUser authenticate(Object id, Object version) {
    if (!(id instanceof Number userId) || !(version instanceof Number sessionVersion)) return null;
    var user = repository.findById(userId.longValue()).orElse(null);
    if (user == null || !user.isActive() || user.getSessionVersion() != sessionVersion.longValue()) return null;
    return CurrentUser.from(user);
  }
}
