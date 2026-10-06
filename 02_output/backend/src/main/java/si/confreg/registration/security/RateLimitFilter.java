package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;
import si.confreg.registration.application.TimeSource;

/**
 * Per-client limit of {@code APP_RATE_LIMIT_PER_HOUR} requests to {@code /api} in fixed one-hour
 * windows on the system clock (SB-06). In-memory, per instance.
 */
class RateLimitFilter extends OncePerRequestFilter {

  private static final long WINDOW_SECONDS = 3600;
  private static final int MAX_TRACKED_CLIENTS = 100_000;

  private record Window(long index, AtomicInteger count) {}

  private final int limitPerHour;
  private final TimeSource time;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  RateLimitFilter(int limitPerHour, TimeSource time) {
    this.limitPerHour = limitPerHour;
    this.time = time;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long nowSeconds = time.systemNow().getEpochSecond();
    long index = nowSeconds / WINDOW_SECONDS;
    if (windows.size() > MAX_TRACKED_CLIENTS) {
      windows.values().removeIf(window -> window.index() != index);
    }
    Window window =
        windows.compute(
            request.getRemoteAddr(),
            (client, current) ->
                current == null || current.index() != index
                    ? new Window(index, new AtomicInteger())
                    : current);
    if (window.count().incrementAndGet() > limitPerHour) {
      long retryAfter = (index + 1) * WINDOW_SECONDS - nowSeconds;
      response.setHeader("Retry-After", Long.toString(retryAfter));
      JsonError.write(response, 429, "rate_limited");
      return;
    }
    chain.doFilter(request, response);
  }
}
