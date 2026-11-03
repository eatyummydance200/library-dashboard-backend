package dev.library.security;

import jakarta.servlet.http.HttpServletRequest;

public final class CsrfTokens {
  private CsrfTokens() {}

  public static String get(HttpServletRequest request) {
    var session = request.getSession();
    if (session.getAttribute("csrf") == null) session.setAttribute("csrf", Passwords.token());
    return (String) session.getAttribute("csrf");
  }
}
