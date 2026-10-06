package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed one-hour window per client address over every {@code /api/**} request, which covers the
 * public registration endpoint and organizer authentication (SB-06). In memory, single instance.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_MILLIS = Duration.ofHours(1).toMillis();
  private static final int CLEANUP_THRESHOLD = 10_000;

  private final int limitPerHour;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(int limitPerHour, Clock clock) {
    this.limitPerHour = limitPerHour;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.millis();
    long windowStart = now - now % WINDOW_MILLIS;
    if (windows.size() > CLEANUP_THRESHOLD) {
      windows.values().removeIf(window -> window.start < windowStart);
    }
    Window window =
        windows.compute(
            request.getRemoteAddr(),
            (client, current) ->
                current == null || current.start != windowStart
                    ? new Window(windowStart)
                    : current);
    if (window.count.incrementAndGet() > limitPerHour) {
      long retryAfterSeconds = Math.max(1, (windowStart + WINDOW_MILLIS - now + 999) / 1000);
      response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
      ProblemResponses.write(
          response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
      return;
    }
    chain.doFilter(request, response);
  }

  private static final class Window {
    private final long start;
    private final AtomicInteger count = new AtomicInteger();

    private Window(long start) {
      this.start = start;
    }
  }
}
