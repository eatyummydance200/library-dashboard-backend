package dev.library.common.exception;

import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;

public final class DatabaseErrors {
  private DatabaseErrors() {}

  public static AppException from(RuntimeException error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException sql) {
        String state = sql.getSQLState() == null ? "" : sql.getSQLState();
        if (state.startsWith("23")) return conflict();
        if (state.startsWith("40") || state.equals("55P03")) return concurrent();
      }
    }
    if (error instanceof DataIntegrityViolationException) return conflict();
    if (error instanceof ConcurrencyFailureException) return concurrent();
    LoggerFactory.getLogger(DatabaseErrors.class).error("Database operation failed", error);
    return new AppException(503, "저장소에 접근하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }

  private static AppException conflict() {
    return new AppException(409, "같은 이름이 있거나 입력값이 올바르지 않습니다.");
  }

  private static AppException concurrent() {
    return new AppException(409, "다른 요청과 충돌했습니다. 새로고침 후 다시 시도해주세요.");
  }
}
