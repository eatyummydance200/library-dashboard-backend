package dev.library.user;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @GetMapping("/api/users")
  Object users(HttpServletRequest r) {
    return service.users((CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/users")
  ResponseEntity<?> addUser(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.addUser(d, (CurrentUser) r.getAttribute("user")));
  }

  @PatchMapping("/api/users/{id}")
  Object saveUser(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveUser(id, d, (CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/password")
  Object changePassword(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    var result = service.changePassword(d, (CurrentUser) r.getAttribute("user"));
    r.getSession().invalidate();
    return result;
  }
}
