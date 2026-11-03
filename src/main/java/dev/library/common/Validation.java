package dev.library.common;

import java.time.*;
import java.util.*;
import dev.library.common.exception.AppException;

public final class Validation {
  public static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

  private Validation() {}

  public static void fail(String message) {
    throw new AppException(400, message);
  }

  public static int number(Object value) {
    return ((Number) value).intValue();
  }

  public static boolean integer(Object v) {
    return v instanceof Integer || v instanceof Long;
  }

  public static String text(Object value, String label, int maximum, boolean required) {
    if (!(value instanceof String)) throw new AppException(400, label + "을(를) 확인해주세요.");
    String s = ((String) value).strip();
    if (s.length() > maximum || required && s.isEmpty()) fail(label + "을(를) 확인해주세요.");
    return s;
  }

  public static String password(Object v) {
    if (!(v instanceof String s) || s.isEmpty() || s.length() > 128)
      throw new AppException(400, "비밀번호를 확인해주세요.");
    return (String) v;
  }

  public static LocalDate day(Object v) {
    try {
      if (!(v instanceof String s) || !s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
        throw new IllegalArgumentException();
      LocalDate d = LocalDate.parse((String) v);
      if (d.getYear() < 1) throw new IllegalArgumentException();
      return d;
    } catch (Exception e) {
      throw new AppException(400, "날짜를 확인해주세요.");
    }
  }

  public static LocalDate monday(String value) {
    LocalDate d = day(value);
    return d.minusDays(d.getDayOfWeek().getValue() - 1);
  }

  public static String today() {
    return LocalDate.now(KOREA).toString();
  }

  public static String stamp() {
    return Instant.now().toString();
  }

  public static Map<String, Object> ok() {
    return Map.of("ok", true);
  }

  public static String[] person(Map<String, Object> d) {
    String name = text(d.get("name"), "이름", 20, true),
        phone = text(d.getOrDefault("phone", ""), "전화번호", 20, false);
    if (!phone.isEmpty() && !phone.matches("[0-9+() -]{7,20}")) fail("전화번호 형식을 확인해주세요.");
    return new String[] {name, phone};
  }

  public static void period(int year, int month) {
    if (year < 2000 || year > 2100 || month < 1 || month > 12) fail("연도와 월을 확인해주세요.");
  }

  public static boolean bool(Object v, String error) {
    if (!(v instanceof Boolean)) fail(error);
    return (Boolean) v;
  }

  public static long id(Object value, String label) {
    if (!integer(value) || ((Number) value).longValue() < 1)
      throw new AppException(400, label + "을(를) 확인해주세요.");
    return ((Number) value).longValue();
  }
}
