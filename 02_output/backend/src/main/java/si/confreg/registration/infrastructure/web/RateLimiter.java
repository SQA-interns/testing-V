package si.confreg.registration.infrastructure.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.application.TimeSource;

/**
 * Fixed one-hour windows per client and bucket, in memory (single instance; SB-06, D-16). A window
 * starts with the client's first counted request.
 */
@Component
public class RateLimiter {

  static final Duration WINDOW = Duration.ofHours(1);
  private static final int MAX_TRACKED = 100_000;

  /** Independent allowances, so price refreshes cannot use up the registration allowance. */
  public enum Bucket {
    REGISTER,
    OPTIONS,
    AUTH_FAILURE
  }

  private record Key(Bucket bucket, String client) {}

  private static final class Window {
    private final Instant start;
    private int count;

    Window(Instant start) {
      this.start = start;
    }
  }

  private final Map<Key, Window> windows = new ConcurrentHashMap<>();
  private final AppProperties properties;
  private final TimeSource timeSource;

  public RateLimiter(AppProperties properties, TimeSource timeSource) {
    this.properties = properties;
    this.timeSource = timeSource;
  }

  /**
   * Counts one request and returns 0 when it is allowed, otherwise the seconds until the window
   * ends.
   */
  public long acquire(Bucket bucket, String client) {
    return update(bucket, client, true);
  }

  /** Returns 0 when the bucket still has room, otherwise the seconds until the window ends. */
  public long check(Bucket bucket, String client) {
    return update(bucket, client, false);
  }

  private long update(Bucket bucket, String client, boolean count) {
    Instant now = timeSource.now();
    if (windows.size() > MAX_TRACKED) {
      windows.values().removeIf(w -> !now.isBefore(w.start.plus(WINDOW)));
    }
    long[] retryAfter = {0};
    windows.compute(
        new Key(bucket, client),
        (key, window) -> {
          Window current =
              window == null || !now.isBefore(window.start.plus(WINDOW)) ? new Window(now) : window;
          if (current.count >= properties.rateLimitPerHour()) {
            retryAfter[0] =
                Math.max(1, Duration.between(now, current.start.plus(WINDOW)).toSeconds());
          } else if (count) {
            current.count++;
          }
          return current;
        });
    return retryAfter[0];
  }
}
