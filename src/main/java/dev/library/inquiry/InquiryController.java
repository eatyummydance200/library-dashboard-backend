package dev.library.inquiry;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class InquiryController {
  private final InquiryService service;

  public InquiryController(InquiryService service) {
    this.service = service;
  }

  @PostMapping("/api/inquiries")
  ResponseEntity<?> inquiry(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.inquiry(d, (CurrentUser) r.getAttribute("user")));
  }
}
