package dev.library.duty;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class DutyController {
  private final DutyService service;

  public DutyController(DutyService service) {
    this.service = service;
  }

  @GetMapping("/api/duty")
  Object duty() {
    return service.duty();
  }

  @PutMapping("/api/duty")
  Object saveDuty(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveDuty(d, (CurrentUser) r.getAttribute("user"));
  }
}
