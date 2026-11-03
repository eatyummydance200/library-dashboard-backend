package dev.library.staff;

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
public class StaffService {
  private final StaffRepository repository;
  private final WriteLock lock;

  public StaffService(StaffRepository repository, WriteLock lock) {
    this.repository = repository;
    this.lock = lock;
  }

  public List<StaffResponse> staff() {
    return repository.findAllByOrderByIdAsc().stream().map(StaffResponse::from).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object addStaff(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    var values = person(body);
    lock.acquire();
    var staff = new Staff();
    staff.setName(values[0]);
    staff.setPhone(values[1]);
    repository.save(staff);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveStaff(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    var values = person(body);
    boolean active = bool(body.getOrDefault("active", true), "재직 상태를 확인해주세요.");
    lock.acquire();
    var staff = repository.findById(id).orElseThrow(() -> new AppException(404, "근무자를 찾을 수 없습니다."));
    staff.setName(values[0]);
    staff.setPhone(values[1]);
    staff.setActive(active);
    return ok();
  }
}
