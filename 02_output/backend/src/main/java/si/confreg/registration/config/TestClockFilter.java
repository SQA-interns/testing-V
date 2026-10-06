package si.confreg.registration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds the instant from {@code X-Test-Now} for the request when the test clock is enabled (AR-05);
 * ignores the header otherwise.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestClockFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Test-Now";

  private final AppSettings settings;
  private final ConferenceClock clock;

  public TestClockFilter(AppSettings settings, ConferenceClock clock) {
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !settings.testClockEnabled() || request.getHeader(HEADER) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Instant testNow;
    try {
      testNow = Instant.parse(request.getHeader(HEADER).trim());
    } catch (DateTimeParseException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response
          .getWriter()
          .write("{\"status\":400,\"title\":\"Bad Request\",\"detail\":\"Invalid X-Test-Now\"}");
      return;
    }
    clock.bindTestNow(testNow);
    try {
      chain.doFilter(request, response);
    } finally {
      clock.clearTestNow();
    }
  }
}
