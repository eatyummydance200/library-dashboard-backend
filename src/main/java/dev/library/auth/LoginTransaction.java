package dev.library.auth;

import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.Passwords;
import dev.library.user.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginTransaction {
  private final AuthRepository attempts;
  private final UserRepository users;
  private final WriteLock lock;
  private final String dummy = Passwords.encode(Passwords.token());

  public LoginTransaction(AuthRepository attempts, UserRepository users, WriteLock lock) {
    this.attempts = attempts;
    this.users = users;
    this.lock = lock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public AuthenticatedUser authenticate(String name, String password) {
    lock.acquire();
    long now = Instant.now().getEpochSecond();
    attempts.deleteExpiredAttempts(now - 900);
    if (attempts.countAttempts(name) >= 5)
      throw new AppException(429, "로그인 시도가 많습니다. 15분 후 다시 시도해주세요.");
    var user = users.findByUsername(name).orElse(null);
    boolean matches = Passwords.matches(user == null ? dummy : user.getPasswordHash(), password);
    if (user == null || !user.isActive() || !matches) {
      attempts.insertAttempt(name, now);
      return null;
    }
    attempts.deleteAttempts(name);
    return new AuthenticatedUser(user.getId(), user.getSessionVersion());
  }
}
