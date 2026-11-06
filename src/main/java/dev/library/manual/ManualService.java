package dev.library.manual;

import static dev.library.common.Validation.*;

import dev.library.common.db.WriteLock;
import dev.library.common.exception.AppException;
import dev.library.security.CurrentUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ManualService {
  static final int MAX_CONTENT = 200_000;
  static final long MAX_IMAGE = 5 * 1024 * 1024;
  static final String TOO_LARGE = "이미지는 5MB 이하만 올릴 수 있습니다.";
  private static final String NAME = "[A-Za-z0-9._-]{1,255}";
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/png", "png", "image/jpeg", "jpg", "image/gif", "gif", "image/webp", "webp");
  private static final Map<String, String> SEED_TYPES =
      Map.of("png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "gif", "image/gif", "webp", "image/webp");

  private final ManualRepository repository;
  private final ManualImageRepository images;
  private final WriteLock lock;

  public ManualService(ManualRepository repository, ManualImageRepository images, WriteLock lock) {
    this.repository = repository;
    this.images = images;
    this.lock = lock;
  }

  public ManualResponse manual() {
    return repository.findById(1L).map(ManualResponse::from).orElse(ManualResponse.EMPTY);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ManualResponse update(Map<String, Object> body, CurrentUser actor) {
    actor.requireAdmin();
    String content = text(body.get("content"), "매뉴얼 내용", MAX_CONTENT, false);
    lock.acquire();
    var manual = repository.findById(1L).orElse(null);
    if (manual == null) manual = repository.save(new Manual(content, Instant.now(), actor.username()));
    else manual.update(content, Instant.now(), actor.username());
    return ManualResponse.from(manual);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, String> upload(String declaredType, byte[] data, CurrentUser actor) {
    actor.requireAdmin();
    if (data.length > MAX_IMAGE) throw new AppException(413, TOO_LARGE);
    String type = sniff(data);
    if (type == null || declaredType == null || !type.equals(declaredType.toLowerCase(Locale.ROOT)))
      fail("PNG, JPEG, GIF, WEBP 이미지만 올릴 수 있습니다.");
    String name = UUID.randomUUID() + "." + EXTENSIONS.get(type);
    lock.acquire();
    images.save(new ManualImage(name, type, data, Instant.now()));
    return Map.of("name", name);
  }

  public ManualImage image(String name) {
    if (!name.matches(NAME)) fail("이미지 이름을 확인해주세요.");
    return images.findById(name).orElseThrow(() -> new AppException(404, "이미지를 찾을 수 없습니다."));
  }

  /** Runs only when the manual row is missing; image names that already exist are kept. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void seed(Path dir) {
    lock.acquire();
    if (repository.existsById(1L)) return;
    try {
      String content = Files.readString(dir.resolve("manual.md"), StandardCharsets.UTF_8);
      if (content.length() > MAX_CONTENT)
        throw new IllegalStateException("manual.md exceeds " + MAX_CONTENT + " characters");
      Path folder = dir.resolve("images");
      if (Files.isDirectory(folder)) {
        try (var files = Files.list(folder)) {
          for (Path file : files.filter(Files::isRegularFile).toList()) {
            String name = file.getFileName().toString();
            String type = SEED_TYPES.get(name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
            if (type == null || !name.matches(NAME)) {
              LoggerFactory.getLogger(ManualService.class).warn("Skipping manual seed file {}", name);
              continue;
            }
            if (!images.existsById(name))
              images.save(new ManualImage(name, type, Files.readAllBytes(file), Instant.now()));
          }
        }
      }
      repository.save(new Manual(content, Instant.now(), "seed"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Content type from magic bytes, so a renamed HTML or SVG file cannot pass as an image. */
  private static String sniff(byte[] d) {
    if (starts(d, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
    if (starts(d, 0, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
    if (starts(d, 0, 'G', 'I', 'F', '8')) return "image/gif";
    if (starts(d, 0, 'R', 'I', 'F', 'F') && starts(d, 8, 'W', 'E', 'B', 'P')) return "image/webp";
    return null;
  }

  private static boolean starts(byte[] d, int offset, int... signature) {
    if (d.length < offset + signature.length) return false;
    for (int i = 0; i < signature.length; i++) if ((d[offset + i] & 0xFF) != signature[i]) return false;
    return true;
  }
}
