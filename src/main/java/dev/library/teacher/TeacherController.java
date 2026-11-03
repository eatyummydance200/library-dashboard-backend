package dev.library.teacher;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeacherController {
  private final TeacherService service;

  public TeacherController(TeacherService service) {
    this.service = service;
  }

  @GetMapping("/api/teachers")
  Object teachers() {
    return service.teachers();
  }

  @PostMapping("/api/teachers")
  ResponseEntity<?> addTeacher(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.addTeacher(d, (CurrentUser) r.getAttribute("user")));
  }

  @PutMapping("/api/teachers/{id}")
  Object saveTeacher(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveTeacher(id, d, (CurrentUser) r.getAttribute("user"));
  }
}
