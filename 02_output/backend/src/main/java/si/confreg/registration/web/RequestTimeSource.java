package si.confreg.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import si.confreg.registration.application.TimeSource;

/**
 * The one clock component (AR-05, spec 6). System UTC time, unless the test clock is enabled and
 * the current request carries {@code X-Test-Now}. As a filter it parses the header once per request
 * and answers 400 for a malformed value.
 */
public class RequestTimeSource extends OncePerRequestFilter implements TimeSource {

  public static final String HEADER = "X-Test-Now";

  /** Test instant of the request being processed on this thread (set and cleared by the filter). */
  private final ThreadLocal<Instant> requestInstant = new ThreadLocal<>();

  private final boolean testClockEnabled;
  private final Clock systemClock;

  public RequestTimeSource(boolean testClockEnabled) {
    this(testClockEnabled, Clock.systemUTC());
  }

  RequestTimeSource(boolean testClockEnabled, Clock systemClock) {
    this.testClockEnabled = testClockEnabled;
    this.systemClock = systemClock;
  }

  @Override
  public Instant now() {
    Instant testNow = requestInstant.get();
    return testNow != null ? testNow : systemClock.instant();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = testClockEnabled ? request.getHeader(HEADER) : null;
    if (header == null) {
      chain.doFilter(request, response);
      return;
    }
    Instant testNow;
    try {
      testNow = Instant.parse(header.strip());
    } catch (DateTimeParseException e) {
      Problems.write(response, HttpStatus.BAD_REQUEST);
      return;
    }
    requestInstant.set(testNow);
    try {
      chain.doFilter(request, response);
    } finally {
      requestInstant.remove();
    }
  }
}
