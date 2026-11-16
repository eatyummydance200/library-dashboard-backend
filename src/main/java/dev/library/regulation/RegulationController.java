package dev.library.regulation;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegulationController {
  private final RegulationService service;
  private final RegulationQuestionService questions;

  public RegulationController(RegulationService service, RegulationQuestionService questions) {
    this.service = service;
    this.questions = questions;
  }

  @GetMapping("/api/regulations")
  Object regulations() {
    return service.regulations();
  }

  @PutMapping("/api/regulations")
  Object update(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.update(d, (CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/regulations/questions")
  ResponseEntity<?> ask(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(questions.ask(d, (CurrentUser) r.getAttribute("user")));
  }

  @PatchMapping("/api/regulations/questions/{id}")
  Object mark(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return questions.update(id, d, (CurrentUser) r.getAttribute("user"));
  }

  @GetMapping("/api/regulations/questions")
  Object questions(@RequestParam(required = false) String filter, HttpServletRequest r) {
    return questions.questions(filter, (CurrentUser) r.getAttribute("user"));
  }
}
