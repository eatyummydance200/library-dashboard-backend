package dev.library.duty;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.library.teacher.Teacher;
import dev.library.teacher.TeacherRepository;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DutyService {
  private final DutyContactRepository repository;
  private final TeacherRepository teachers;
  private final WriteLock lock;

  public DutyService(DutyContactRepository repository, TeacherRepository teachers, WriteLock lock) {
    this.repository = repository;
    this.teachers = teachers;
    this.lock = lock;
  }

  public DutyResponse duty() {
    return repository.findByDay(LocalDate.now(KOREA)).map(this::response)
        .orElse(new DutyResponse(0L, "", "", ""));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public DutyResponse saveDuty(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    if (!body.containsKey("teacher_id")) fail("당직 선생님을 선택해주세요.");
    lock.acquire();
    Teacher teacher = body.get("teacher_id") == null ? null : teachers.findById(id(body.get("teacher_id"), "당직 선생님"))
        .orElseThrow(() -> new AppException(404, "선생님을 찾을 수 없습니다."));
    var contact = repository.findById(1L).orElseGet(DutyContact::new);
    contact.setSelectedTeacher(teacher);
    contact.setTeacher(teacher == null ? "" : teacher.getName());
    contact.setExtension(teacher == null ? "" : teacher.getExtension());
    contact.setPhone(teacher == null ? "" : teacher.getPhone());
    contact.setDay(LocalDate.now(KOREA));
    repository.save(contact);
    return response(contact);
  }

  private DutyResponse response(DutyContact contact) {
    var teacher = contact.getSelectedTeacher();
    return teacher == null
        ? new DutyResponse(0L, contact.getTeacher(), contact.getExtension(), contact.getPhone())
        : new DutyResponse(teacher.getId(), teacher.getName(), teacher.getExtension(), teacher.getPhone());
  }
}
