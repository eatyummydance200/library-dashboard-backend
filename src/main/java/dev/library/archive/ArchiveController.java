package dev.library.archive;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArchiveController {
  private final ArchiveService service;

  public ArchiveController(ArchiveService service) {
    this.service = service;
  }

  @GetMapping("/api/archive")
  Object archive(@RequestParam(required = false) String month) {
    return service.archive(month);
  }

  @PostMapping("/api/archive/draw")
  ResponseEntity<?> draw(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.draw(d, (CurrentUser) r.getAttribute("user")));
  }

  @PutMapping("/api/archive/assignments")
  Object manual(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.manual(d, (CurrentUser) r.getAttribute("user"));
  }

  @PutMapping("/api/archive/tasks/{id}")
  Object complete(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.complete(id, d, (CurrentUser) r.getAttribute("user"));
  }
}
