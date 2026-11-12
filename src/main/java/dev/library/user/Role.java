package dev.library.user;

import java.util.Locale;

public enum Role {
  ADMIN, WORKER, VIEWER;

  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
