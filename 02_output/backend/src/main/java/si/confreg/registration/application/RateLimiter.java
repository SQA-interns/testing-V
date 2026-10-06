package si.confreg.registration.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key sliding one-hour window (spec 7.4, SB-06, AC-001-12). Time comes from {@link TimeSource},
 * so the test clock applies. Rejected requests are not counted. In memory: one backend instance.
 */
public final class RateLimiter {

  static final Duration WINDOW = Duration.ofHours(1);
  static final int MAX_KEYS = 100_000;

  private final int limit;
  private final TimeSource time;
  private final Map<String, List<Instant>> hits = new ConcurrentHashMap<>();

  public RateLimiter(int limitPerHour, TimeSource time) {
    if (limitPerHour < 1) {
      throw new IllegalArgumentException("rate limit must be at least 1 per hour");
    }
    this.limit = limitPerHour;
    this.time = time;
  }

  /** Outcome of {@link #tryAcquire}: allowed, or rejected with seconds until a slot frees up. */
  public record Decision(boolean allowed, long retryAfterSeconds) {}

  /** Counts one request for {@code key} if the key is under its limit. */
  public Decision tryAcquire(String key) {
    Instant now = time.now();
    List<Instant> window = window(key);
    synchronized (window) {
      prune(window, now);
      long inWindow = window.stream().filter(t -> !t.isAfter(now)).count();
      if (inWindow >= limit) {
        return new Decision(false, retryAfter(window, now));
      }
      window.add(now);
      return new Decision(true, 0);
    }
  }

  /** Whether {@code key} has used up its limit (without counting a request). */
  public Decision check(String key) {
    Instant now = time.now();
    List<Instant> window = window(key);
    synchronized (window) {
      prune(window, now);
      long inWindow = window.stream().filter(t -> !t.isAfter(now)).count();
      return inWindow >= limit
          ? new Decision(false, retryAfter(window, now))
          : new Decision(true, 0);
    }
  }

  /** Counts one event for {@code key} unconditionally (for example a failed login). */
  public void record(String key) {
    Instant now = time.now();
    List<Instant> window = window(key);
    synchronized (window) {
      prune(window, now);
      window.add(now);
    }
  }

  private List<Instant> window(String key) {
    if (hits.size() > MAX_KEYS) {
      hits.clear();
    }
    return hits.computeIfAbsent(key, k -> new ArrayList<>());
  }

  private static void prune(List<Instant> window, Instant now) {
    Instant oldest = now.minus(WINDOW);
    window.removeIf(t -> !t.isAfter(oldest));
  }

  private static long retryAfter(List<Instant> window, Instant now) {
    Instant earliest =
        window.stream().filter(t -> !t.isAfter(now)).min(Instant::compareTo).orElse(now);
    long seconds = Duration.between(now, earliest.plus(WINDOW)).toSeconds();
    return Math.max(1, seconds);
  }
}
