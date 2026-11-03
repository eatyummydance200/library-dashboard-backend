package dev.library.security;

import dev.library.common.exception.AppException;
import dev.library.common.exception.DatabaseErrors;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionSystemException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
class AuthFilter extends OncePerRequestFilter {
  private final SessionAuthService service;
  private final ObjectMapper mapper;

  @Value("${management.server.port:8081}")
  private int managementPort;

  @Value("${server.port:8080}")
  private int applicationPort;

  AuthFilter(SessionAuthService service, ObjectMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // The separate management context has its own bearer-token filter.
    return managementPort != applicationPort && request.getLocalPort() == managementPort;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    try {
      HttpSession session = request.getSession(false);
      CurrentUser user = null;
      if (session != null && session.getAttribute("user_id") != null) {
        user = service.authenticate(session.getAttribute("user_id"), session.getAttribute("version"));
        if (user == null) {
          session.invalidate();
        }
      }
      request.setAttribute("user", user);
      boolean publicPath =
          path.equals("/api/auth/csrf")
              || path.equals("/api/auth/login")
              || path.equals("/healthz")
              || path.equals("/actuator/health/liveness")
              || path.equals("/actuator/health/readiness");
      if (!publicPath && user == null) throw new AppException(401, "로그인이 필요합니다.");
      chain.doFilter(request, response);
    } catch (AppException e) {
      failure(response, e);
    } catch (DataAccessException | PersistenceException | TransactionSystemException e) {
      failure(response, DatabaseErrors.from(e));
    }
  }

  private void failure(HttpServletResponse response, AppException error) throws IOException {
    response.setStatus(error.status());
    response.setContentType("application/json;charset=UTF-8");
    mapper.writeValue(response.getWriter(), Map.of("error", error.getMessage()));
  }
}
