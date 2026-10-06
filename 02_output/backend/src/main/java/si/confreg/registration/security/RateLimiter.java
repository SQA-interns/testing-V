package si.confreg.registration.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import si.confreg.registration.config.AppSettings;
import si.confreg.registration.config.ConferenceClock;

/**
 * Sliding one-hour window per bucket and client, limited to {@code APP_RATE_LIMIT_PER_HOUR} (SB-06,
 * D-11). In memory, single instance; client addresses are kept at most one hour and never stored
 * (SB-12).
 */
@Component
public class RateLimiter {

  static final Duration WINDOW = Duration.ofHours(1);
  static final int MAX_TRACKED = 100_000;

  private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
  private final AppSettings settings;
  private final ConferenceClock clock;

  public RateLimiter(AppSettings settings, ConferenceClock clock) {
    this.settings = settings;
    this.clock = clock;
  }

  /**
   * Counts one request if the client is under the limit.
   *
   * @return empty if allowed, otherwise the seconds until the client may retry
   */
  public OptionalLong tryAcquire(String bucket, String client) {
    Instant now = clock.now();
    Deque<Instant> window = window(bucket, client);
    synchronized (window) {
      prune(window, now);
      if (window.size() >= settings.rateLimitPerHour()) {
        return OptionalLong.of(retryAfter(window, now));
      }
      window.addLast(now);
      return OptionalLong.empty();
    }
  }

  /** Counts one event (for example a failed authentication) without checking the limit. */
  public void record(String bucket, String client) {
    Instant now = clock.now();
    Deque<Instant> window = window(bucket, client);
    synchronized (window) {
      prune(window, now);
      window.addLast(now);
    }
  }

  /**
   * @return the seconds until the client may retry if its budget is used up, otherwise empty
   */
  public OptionalLong exhausted(String bucket, String client) {
    Deque<Instant> window = hits.get(bucket + '|' + client);
    if (window == null) {
      return OptionalLong.empty();
    }
    Instant now = clock.now();
    synchronized (window) {
      prune(window, now);
      return window.size() >= settings.rateLimitPerHour()
          ? OptionalLong.of(retryAfter(window, now))
          : OptionalLong.empty();
    }
  }

  private Deque<Instant> window(String bucket, String client) {
    if (hits.size() >= MAX_TRACKED) {
      evictIdle();
    }
    return hits.computeIfAbsent(bucket + '|' + client, key -> new ArrayDeque<>());
  }

  private void evictIdle() {
    Instant cutoff = clock.now().minus(WINDOW);
    hits.entrySet()
        .removeIf(
            entry -> {
              synchronized (entry.getValue()) {
                Instant last = entry.getValue().peekLast();
                return last == null || !last.isAfter(cutoff);
              }
            });
    if (hits.size() >= MAX_TRACKED) {
      hits.clear();
    }
  }

  private static void prune(Deque<Instant> window, Instant now) {
    Instant cutoff = now.minus(WINDOW);
    while (!window.isEmpty() && !window.peekFirst().isAfter(cutoff)) {
      window.removeFirst();
    }
  }

  private static long retryAfter(Deque<Instant> window, Instant now) {
    Instant oldest = window.peekFirst();
    long seconds = oldest == null ? 1 : Duration.between(now, oldest.plus(WINDOW)).toSeconds();
    return Math.max(1, seconds);
  }
}
