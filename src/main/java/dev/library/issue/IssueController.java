package dev.library.issue;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class IssueController {
  private final IssueService service;

  public IssueController(IssueService service) {
    this.service = service;
  }

  @GetMapping("/api/issues")
  Object issues(
      @RequestParam(required = false) String day,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String week) {
    return service.issues(day, category, week);
  }

  @PostMapping("/api/issues")
  ResponseEntity<?> issue(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.issue(d, (CurrentUser) r.getAttribute("user")));
  }

  @PatchMapping("/api/issues/{id}")
  Object toggleIssue(@PathVariable long id, @RequestBody Map<String, Object> d) {
    return service.toggleIssue(id, d);
  }

  @PostMapping("/api/issues/{id}/apply")
  Object apply(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.apply(id, d, (CurrentUser) r.getAttribute("user"));
  }
}
