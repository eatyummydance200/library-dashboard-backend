package dev.library.user;

import dev.library.common.Validation;

import static dev.library.common.Validation.*;
import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.library.staff.Staff;
import dev.library.staff.StaffRepository;
import dev.library.security.Passwords;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class UserService {
  private final UserRepository repository;
  private final StaffRepository staffRepository;
  private final WriteLock lock;

  public UserService(UserRepository repository, StaffRepository staffRepository, WriteLock lock) {
    this.repository = repository;
    this.staffRepository = staffRepository;
    this.lock = lock;
  }

  public List<UserResponse> users(CurrentUser actor) {
    actor.requireAdmin();
    return repository.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
  }

  private Staff linked(Object value) {
    if (value == null) return null;
    return staffRepository.findById(id(value, "연결할 근무자"))
        .orElseThrow(() -> new AppException(400, "연결할 근무자를 확인해주세요."));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object addUser(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    String name = text(body.get("username"), "아이디", 32, true);
    String password = password(body.get("password"));
    if (!name.matches("[A-Za-z0-9_.-]{2,32}") || password.length() < 10)
      fail("아이디는 영문·숫자 2~32자, 비밀번호는 10자 이상으로 입력해주세요.");
    Object role = body.getOrDefault("role", "worker");
    if (role == null || !List.of("admin", "worker").contains(role)) fail("권한을 확인해주세요.");
    lock.acquire();
    var user = new User();
    user.setUsername(name);
    user.setPasswordHash(Passwords.encode(password));
    user.setRole(Role.valueOf(((String) role).toUpperCase(Locale.ROOT)));
    user.setStaff(linked(body.get("staff_id")));
    repository.save(user);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object saveUser(long id, Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    lock.acquire();
    var user = repository.findById(id).orElseThrow(() -> new AppException(404, "계정을 찾을 수 없습니다."));
    if (body.isEmpty() || !Set.of("active", "staff_id").containsAll(body.keySet())) fail("계정 변경 항목을 확인해주세요.");
    boolean active = bool(body.getOrDefault("active", user.isActive()), "본인 계정은 비활성화할 수 없습니다.");
    if (id == actor.id() && !active) fail("본인 계정은 비활성화할 수 없습니다.");
    user.setActive(active);
    if (body.containsKey("staff_id")) user.setStaff(linked(body.get("staff_id")));
    user.setSessionVersion(user.getSessionVersion() + 1);
    return ok();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Object changePassword(Map<String, Object> body, CurrentUser actor) {
    String old = password(body.get("current")), password = password(body.get("password"));
    lock.acquire();
    var user = repository.findById(actor.id()).orElseThrow(() -> new AppException(401, "로그인이 필요합니다."));
    if (password.length() < 10 || !Passwords.matches(user.getPasswordHash(), old))
      fail("현재 비밀번호를 확인하고 새 비밀번호는 10자 이상 입력해주세요.");
    user.setPasswordHash(Passwords.encode(password));
    user.setSessionVersion(user.getSessionVersion() + 1);
    return ok();
  }
}
