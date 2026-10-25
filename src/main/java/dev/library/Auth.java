package dev.library;

import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
class Auth {
  final Db db;
  private final String dummy = Passwords.encode(Passwords.token());

  Auth(Db db) {
    this.db = db;
  }

  @GetMapping("/api/auth/csrf")
  Object csrf(HttpServletRequest r) {
    return Map.of("csrf", AuthFilter.csrf(r));
  }

  @PostMapping("/api/auth/login")
  Object loginJson(@RequestBody Map<String, Object> body, HttpServletRequest r) {
    authenticate(r, Api.text(body.get("username"), "아이디", 32, true),
        Api.password(body.get("password")));
    return Map.of("csrf", AuthFilter.csrf(r));
  }

  private void authenticate(HttpServletRequest r, String name, String password) {
    long now = Instant.now().getEpochSecond();
    // Commit failed attempts before returning 401; throwing here would roll them back.
    var user = db.write(c -> {
      Db.update(c, "DELETE FROM login_attempts WHERE attempted_at<?", now - 900);
      int attempts = Api.number(
          Db.one(c, "SELECT count(*) AS n FROM login_attempts WHERE username=?", name).get("n"));
      if (attempts >= 5)
        throw new Api.Failure(429, "로그인 시도가 많습니다. 15분 후 다시 시도해주세요.");
      var found = Db.one(c, "SELECT * FROM users WHERE username=?", name);
      boolean valid = Passwords.matches(
          found == null ? dummy : (String) found.get("password_hash"), password);
      if (found == null || Api.number(found.get("active")) == 0 || !valid) {
        Db.update(c, "INSERT INTO login_attempts VALUES (?,?)", name, now);
        return null;
      }
      Db.update(c, "DELETE FROM login_attempts WHERE username=?", name);
      return found;
    });
    if (user == null) throw new Api.Failure(401, "아이디 또는 비밀번호를 확인해주세요.");
    r.getSession().invalidate();
    var session = r.getSession(true);
    session.setAttribute("user_id", user.get("id"));
    session.setAttribute("version", user.get("version"));
    AuthFilter.csrf(r);
  }
}
