package dev.library.schedule;

import java.util.Locale;

public enum ShiftRole {
  DESK, GENERAL;

  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
