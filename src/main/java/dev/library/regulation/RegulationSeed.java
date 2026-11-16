package dev.library.regulation;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads the regulations from a local folder so their contents never live in this public repository. */
@Component
class RegulationSeed implements ApplicationRunner {
  private final RegulationService service;
  private final String dir;

  RegulationSeed(RegulationService service, @Value("${library-dashboard.regulations.seed-dir:}") String dir) {
    this.service = service;
    this.dir = dir;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!dir.isBlank()) service.seed(Path.of(dir));
  }
}
