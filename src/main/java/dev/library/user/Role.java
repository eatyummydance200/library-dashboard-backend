package dev.library.user;

import java.util.Locale;

public enum Role {
  ADMIN, WORKER;

  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
