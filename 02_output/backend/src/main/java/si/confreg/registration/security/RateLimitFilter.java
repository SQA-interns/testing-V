package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.OptionalLong;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits per client address (after trusted forwarded headers): every registration request
 * counts (AC-001-13, D-11); organizer endpoints are blocked while the client's failed
 * authentications use up the budget (SB-06).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

  static final String REGISTRATION = "registration";
  static final String FAILED_AUTHENTICATION = "failed-authentication";
  static final String REGISTRATIONS_PATH = "/api/registrations";

  private final RateLimiter limiter;

  public RateLimitFilter(RateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    String client = request.getRemoteAddr();
    OptionalLong retryAfter = OptionalLong.empty();
    if ("POST".equals(request.getMethod()) && REGISTRATIONS_PATH.equals(path)) {
      retryAfter = limiter.tryAcquire(REGISTRATION, client);
    } else if (path.startsWith("/api/")) {
      retryAfter = limiter.exhausted(FAILED_AUTHENTICATION, client);
    }
    if (retryAfter.isPresent()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter.getAsLong()));
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
