package dev.library.staff;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StaffController {
  private final StaffService service;

  public StaffController(StaffService service) {
    this.service = service;
  }

  @GetMapping("/api/staff")
  Object staff() {
    return service.staff();
  }

  @PostMapping("/api/staff")
  ResponseEntity<?> addStaff(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.addStaff(d, (CurrentUser) r.getAttribute("user")));
  }

  @PutMapping("/api/staff/{id}")
  Object saveStaff(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveStaff(id, d, (CurrentUser) r.getAttribute("user"));
  }
}
