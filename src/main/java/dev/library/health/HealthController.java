package dev.library.health;

import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
  private final HealthService service;

  public HealthController(HealthService service) {
    this.service = service;
  }

  @GetMapping("/healthz")
  Object health() {
    return service.health();
  }
}
