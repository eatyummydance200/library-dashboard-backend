package dev.library.cleaning;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CleaningController {
  private final CleaningService service;

  public CleaningController(CleaningService service) {
    this.service = service;
  }

  @GetMapping("/api/cleaning")
  Object list() {
    return service.list();
  }

  @PostMapping("/api/cleaning/draw")
  ResponseEntity<?> draw(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.draw(d, (CurrentUser) r.getAttribute("user")));
  }

  @PutMapping("/api/cleaning/{month}")
  Object assign(@PathVariable String month, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.assign(month, d, (CurrentUser) r.getAttribute("user"));
  }
}
