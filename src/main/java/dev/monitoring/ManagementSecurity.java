package dev.monitoring;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

// Outside dev.library so this configuration is loaded only in the management context.
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class ManagementSecurity {
  private static final Set<String> PROBES = Set.of(
      "/actuator/health/liveness", "/actuator/health/readiness");

  @Bean
  FilterRegistrationBean<Filter> metricsAccessFilter(
      @Value("${MONITORING_SCRAPE_TOKEN:}") String token) {
    Filter filter = (request, response, chain) -> {
      var http = (HttpServletRequest) request;
      var result = (HttpServletResponse) response;
      result.setHeader("Cache-Control", "no-store");
      result.setHeader("X-Content-Type-Options", "nosniff");
      if (!http.getMethod().equals("GET")) {
        result.setStatus(405);
        return;
      }
      String path = http.getServletPath();
      if (PROBES.contains(path)) {
        chain.doFilter(request, response);
        return;
      }
      if (!path.equals("/actuator/prometheus")) {
        result.setStatus(404);
        return;
      }
      String auth = http.getHeader("Authorization");
      boolean allowed = !token.isBlank() && auth != null && MessageDigest.isEqual(
          ("Bearer " + token).getBytes(StandardCharsets.UTF_8),
          auth.getBytes(StandardCharsets.UTF_8));
      if (!allowed) {
        result.setHeader("WWW-Authenticate", "Bearer");
        result.setStatus(401);
        return;
      }
      chain.doFilter(request, response);
    };
    var registration = new FilterRegistrationBean<>(filter);
    registration.setName("managementMetricsAccess");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
