package dev.library.manual;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ManualController {
  private final ManualService service;

  public ManualController(ManualService service) {
    this.service = service;
  }

  @GetMapping("/api/manual")
  Object manual() {
    return service.manual();
  }

  @PutMapping("/api/manual")
  Object update(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.update(d, (CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/manual/images")
  ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest r) throws IOException {
    var actor = (CurrentUser) r.getAttribute("user");
    actor.requireAdmin();
    if (file.getSize() > ManualService.MAX_IMAGE)
      return ResponseEntity.status(413).body(Map.of("error", ManualService.TOO_LARGE));
    return ResponseEntity.status(201).body(service.upload(file.getContentType(), file.getOriginalFilename(), file.getBytes(), actor));
  }

  @GetMapping("/api/manual/images/{name}")
  void image(@PathVariable String name, HttpServletResponse response) throws IOException {
    var image = service.image(name);
    response.setContentType(image.getContentType());
    response.setContentLength(image.getData().length);
    response.setHeader("Cache-Control", "private, max-age=86400");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.getOutputStream().write(image.getData());
  }
}
