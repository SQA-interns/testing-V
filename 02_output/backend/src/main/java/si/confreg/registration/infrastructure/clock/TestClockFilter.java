package si.confreg.registration.infrastructure.clock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.web.filter.OncePerRequestFilter;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.infrastructure.web.ProblemWriter;

/** Applies {@code X-Test-Now} to the request when {@code APP_TEST_CLOCK=enabled} (AR-05). */
public class TestClockFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Test-Now";

  private final AppProperties properties;

  public TestClockFilter(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    if (!properties.testClockEnabled() || header == null) {
      chain.doFilter(request, response);
      return;
    }
    Instant now;
    try {
      now = Instant.parse(header.trim());
    } catch (DateTimeParseException e) {
      ProblemWriter.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          "Invalid test time",
          "X-Test-Now is invalid.");
      return;
    }
    RequestTimeSource.setRequestNow(now);
    try {
      chain.doFilter(request, response);
    } finally {
      RequestTimeSource.clearRequestNow();
    }
  }
}
