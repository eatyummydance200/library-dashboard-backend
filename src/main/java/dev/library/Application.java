package dev.library;

import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

  public static void main(String[] args) {
    String base = System.getenv().getOrDefault("APP_BASE_PATH", "/library-dashboard");
    if (!base.matches("/[A-Za-z0-9_-]+"))
      throw new IllegalArgumentException("Invalid APP_BASE_PATH");
    SpringApplication.run(Application.class, args);
  }
}
