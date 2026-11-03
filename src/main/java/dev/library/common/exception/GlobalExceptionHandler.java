package dev.library.common.exception;

import java.util.Map;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler({DataAccessException.class, PersistenceException.class, TransactionSystemException.class})
  ResponseEntity<?> database(RuntimeException error) {
    return failure(DatabaseErrors.from(error));
  }

  @ExceptionHandler(AppException.class)
  ResponseEntity<?> failure(AppException e) {
    return ResponseEntity.status(e.status()).body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("error", "입력값을 확인해주세요."));
  }
}
