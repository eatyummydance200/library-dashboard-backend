package dev.library.schedule;

import dev.library.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ScheduleController {
  private final ScheduleService service;

  public ScheduleController(ScheduleService service) {
    this.service = service;
  }

  @GetMapping("/api/schedule")
  Object schedule(@RequestParam(required = false) String week) {
    return service.schedule(week);
  }

  @GetMapping("/api/schedule/original")
  Object originalSchedule(@RequestParam(required = false) String week) {
    return service.originalSchedule(week);
  }

  @PutMapping("/api/schedule/original")
  Object saveOriginalSchedule(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveOriginalSchedule(d, (CurrentUser) r.getAttribute("user"));
  }

  @GetMapping("/api/schedule/hours")
  Object weeklyHours(@RequestParam(required = false) String week) {
    return service.weeklyHours(week);
  }

  @PutMapping("/api/schedule/hours/{id}")
  Object saveWeeklyHours(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveWeeklyHours(id, d, (CurrentUser) r.getAttribute("user"));
  }

  @DeleteMapping("/api/schedule/hours/{id}")
  Object resetWeeklyHours(@PathVariable long id, @RequestParam String week, HttpServletRequest r) {
    return service.resetWeeklyHours(id, week, (CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/shifts")
  ResponseEntity<?> addShift(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return ResponseEntity.status(201).body(service.addShift(d, (CurrentUser) r.getAttribute("user")));
  }

  @PutMapping("/api/shifts/{id}")
  Object saveShift(@PathVariable long id, @RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.saveShift(id, d, (CurrentUser) r.getAttribute("user"));
  }

  @DeleteMapping("/api/shifts/{id}")
  Object deleteShift(@PathVariable long id, HttpServletRequest r) {
    return service.deleteShift(id, (CurrentUser) r.getAttribute("user"));
  }

  @PostMapping("/api/schedule/original/copy")
  Object copy(@RequestBody Map<String, Object> d, HttpServletRequest r) {
    return service.copy(d, (CurrentUser) r.getAttribute("user"));
  }

  @GetMapping("/api/status")
  Object status(@RequestParam(required = false) String at) {
    return service.status(at);
  }
}
