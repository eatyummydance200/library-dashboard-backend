package dev.library.security;

import dev.library.common.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
class RequestProtectionFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    HttpSession session = request.getSession(false);
    if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
      String expected = session == null ? null : (String) session.getAttribute("csrf");
      String supplied = request.getHeader("X-CSRF-Token");
      if (expected == null
          || supplied == null
          || !MessageDigest.isEqual(
              expected.getBytes(StandardCharsets.UTF_8),
              supplied.getBytes(StandardCharsets.UTF_8)))
        throw new AppException(403, "요청이 만료되었습니다. 새로고침 후 다시 시도해주세요.");
      if (request.getContentLengthLong() > 65536) throw new AppException(413, "요청 내용이 너무 큽니다.");
      if (path.startsWith("/api/")) {
        byte[] body = request.getInputStream().readNBytes(65537);
        if (body.length > 65536) throw new AppException(413, "요청 내용이 너무 큽니다.");
        var original = request;
        request =
            new HttpServletRequestWrapper(original) {
              @Override
              public ServletInputStream getInputStream() {
                var in = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                  public int read() {
                    return in.read();
                  }

                  public boolean isFinished() {
                    return in.available() == 0;
                  }

                  public boolean isReady() {
                    return true;
                  }

                  public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                  }
                };
              }

              @Override
              public BufferedReader getReader() {
                return new BufferedReader(
                    new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
              }
            };
      }
    }
    chain.doFilter(request, response);
  }
}
