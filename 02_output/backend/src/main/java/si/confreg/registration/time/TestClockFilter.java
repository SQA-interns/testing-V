package si.confreg.registration.time;

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
 * When the test clock is enabled, takes the current instant for this request from header {@code
 * X-Test-Now} (ISO-8601, UTC). When disabled the header is ignored.
 */
public class TestClockFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Test-Now";

  private final boolean enabled;

  public TestClockFilter(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = enabled ? request.getHeader(HEADER) : null;
    if (header != null) {
      try {
        request.setAttribute(AppClock.TEST_NOW_ATTRIBUTE, Instant.parse(header.trim()));
      } catch (DateTimeParseException e) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response
            .getWriter()
            .write(
                "{\"title\":\"Bad Request\",\"status\":400,"
                    + "\"detail\":\"X-Test-Now must be an ISO-8601 instant.\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
