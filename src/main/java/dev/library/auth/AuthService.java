package dev.library.auth;

import dev.library.common.exception.AppException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final LoginTransaction transaction;

  public AuthService(LoginTransaction transaction) {
    this.transaction = transaction;
  }

  public AuthenticatedUser authenticate(String name, String password) {
    var user = transaction.authenticate(name, password);
    // A failed attempt must commit before the authentication error is thrown.
    if (user == null) throw new AppException(401, "아이디 또는 비밀번호를 확인해주세요.");
    return user;
  }
}
