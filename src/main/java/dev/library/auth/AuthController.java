package dev.library.auth;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;

import dev.library.security.CurrentUser;
import dev.library.security.CsrfTokens;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @GetMapping("/api/auth/csrf")
  Object csrf(HttpServletRequest r) {
    return Map.of("csrf", CsrfTokens.get(r));
  }

  @PostMapping("/api/auth/login")
  Object loginJson(@RequestBody Map<String, Object> body, HttpServletRequest r) {
    var user = service.authenticate(text(body.get("username"), "아이디", 32, true),
        password(body.get("password")));
    r.getSession().invalidate();
    var session = r.getSession(true);
    session.setAttribute("user_id", user.id());
    session.setAttribute("version", user.version());
    return Map.of("csrf", CsrfTokens.get(r));
  }

  @GetMapping("/api/me")
  Object me(HttpServletRequest r) {
    var user = (CurrentUser) r.getAttribute("user");
    var out = new LinkedHashMap<String, Object>();
    out.put("id", user.id());
    out.put("username", user.username());
    out.put("role", user.role());
    out.put("staff_id", user.staffId());
    return Map.of("user", out, "csrf", CsrfTokens.get(r));
  }

  @PostMapping("/api/logout")
  Object logout(HttpServletRequest r) {
    r.getSession().invalidate();
    return ok();
  }
}
