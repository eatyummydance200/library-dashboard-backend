package dev.library.manual;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads the manual from a local folder so its contents never live in this public repository. */
@Component
class ManualSeed implements ApplicationRunner {
  private final ManualService service;
  private final String dir;

  ManualSeed(ManualService service, @Value("${library-dashboard.manual.seed-dir:}") String dir) {
    this.service = service;
    this.dir = dir;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!dir.isBlank()) service.seed(Path.of(dir));
  }
}
