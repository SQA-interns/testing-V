package si.confreg.registration.clock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code X-Test-Now} (ISO-8601 instant) for one request when the test clock is enabled;
 * ignores it otherwise (fixed registration API; SR-04).
 */
public class TestClockFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Test-Now";

  private final TestClockSettings settings;

  public TestClockFilter(TestClockSettings settings) {
    this.settings = settings;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = settings.enabled() ? request.getHeader(HEADER) : null;
    if (header == null || header.isBlank()) {
      chain.doFilter(request, response);
      return;
    }
    Instant testNow;
    try {
      testNow = Instant.parse(header.strip());
    } catch (DateTimeParseException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write("{\"error\":\"invalid_test_clock\"}");
      return;
    }
    ApplicationTimeSource.setTestNow(testNow);
    try {
      chain.doFilter(request, response);
    } finally {
      ApplicationTimeSource.clearTestNow();
    }
  }
}
