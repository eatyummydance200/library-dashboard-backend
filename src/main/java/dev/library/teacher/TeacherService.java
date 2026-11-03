package dev.library.teacher;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class TeacherService {
  private final TeacherRepository repository;
  private final WriteLock lock;

  public TeacherService(TeacherRepository repository, WriteLock lock) {
    this.repository = repository;
    this.lock = lock;
  }

  public List<TeacherResponse> teachers() {
    return repository.findAllByOrderByIdAsc().stream().map(TeacherResponse::from).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object addTeacher(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var teacher = new Teacher();
    apply(teacher, body);
    repository.save(teacher);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveTeacher(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var teacher = repository.findById(id).orElseThrow(() -> new AppException(404, "선생님을 찾을 수 없습니다."));
    apply(teacher, body);
    return ok();
  }

  private void apply(Teacher teacher, Map<String, Object> body) {
    var values = person(body);
    String extension = text(body.getOrDefault("extension", ""), "내선번호", 20, false);
    if (!extension.isEmpty() && !extension.matches("[0-9+() -]{1,20}")) fail("내선번호 형식을 확인해주세요.");
    teacher.setName(values[0]);
    teacher.setPhone(values[1]);
    teacher.setExtension(extension);
    teacher.setPosition(text(body.getOrDefault("position", ""), "직급", 40, false));
    teacher.setLocation(text(body.getOrDefault("location", ""), "근무 장소", 80, false));
  }
}
