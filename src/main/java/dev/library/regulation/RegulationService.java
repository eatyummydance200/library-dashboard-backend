package dev.library.regulation;

import static dev.library.common.Validation.*;

import dev.library.common.db.WriteLock;
import dev.library.security.CurrentUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class RegulationService {
  static final int MAX_CONTENT = 200_000;

  private final RegulationRepository repository;
  private final WriteLock lock;

  public RegulationService(RegulationRepository repository, WriteLock lock) {
    this.repository = repository;
    this.lock = lock;
  }

  public RegulationResponse regulations() {
    return repository.findById(1L).map(RegulationResponse::from).orElse(RegulationResponse.EMPTY);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RegulationResponse update(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    String content = text(body.get("content"), "규정 내용", MAX_CONTENT, true);
    lock.acquire();
    var regulation = repository.findById(1L).orElse(null);
    if (regulation == null) regulation = repository.save(new Regulation(content, Instant.now(), actor.username()));
    else regulation.update(content, Instant.now(), actor.username());
    return RegulationResponse.from(regulation);
  }

  /** Runs only when the regulations row is missing. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void seed(Path dir) {
    lock.acquire();
    if (repository.existsById(1L)) return;
    try {
      String content = Files.readString(dir.resolve("regulations.md"), StandardCharsets.UTF_8).strip();
      if (content.isEmpty() || content.length() > MAX_CONTENT)
        throw new IllegalStateException("regulations.md must have 1 to " + MAX_CONTENT + " characters");
      repository.save(new Regulation(content, Instant.now(), "seed"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
