package dev.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    String base = System.getenv().getOrDefault("APP_BASE_PATH", "/library-dashboard");
    if (!base.matches("/[A-Za-z0-9_-]+"))
      throw new IllegalArgumentException("Invalid APP_BASE_PATH");
    SpringApplication.run(Application.class, args);
  }
}
