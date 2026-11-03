package dev.library.tagging;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaggingController {
  private final TaggingService service;

  public TaggingController(TaggingService service) {
    this.service = service;
  }

  @GetMapping("/api/tagging")
  Object tagging(@RequestParam(required = false) String year) {
    return service.tagging(year);
  }

  @PutMapping("/api/tagging/{id}/{year}/{month}")
  Object saveTag(
      @PathVariable long id,
      @PathVariable int year,
      @PathVariable int month,
      @RequestBody Map<String, Object> d,
      HttpServletRequest r) {
    return service.saveTag(id, year, month, d, (CurrentUser) r.getAttribute("user"));
  }
}
