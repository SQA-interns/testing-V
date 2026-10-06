package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Unit tests of the sliding one-hour window (spec 7.4). */
class RateLimiterTest {

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2031-01-01T00:00:00Z"));
  private final RateLimiter limiter = new RateLimiter(3, now::get);

  @Test
  void allowsUpToTheLimitThenRejectsWithRetryAfter() {
    for (int i = 0; i < 3; i++) {
      assertThat(limiter.tryAcquire("k").allowed()).isTrue();
    }
    RateLimiter.Decision rejected = limiter.tryAcquire("k");

    assertThat(rejected.allowed()).isFalse();
    assertThat(rejected.retryAfterSeconds()).isEqualTo(Duration.ofHours(1).toSeconds());
  }

  @Test
  void rejectedRequestsAreNotCounted() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("k");
    }
    now.set(now.get().plus(Duration.ofMinutes(30)));
    for (int i = 0; i < 5; i++) {
      assertThat(limiter.tryAcquire("k").allowed()).isFalse();
    }
    now.set(now.get().plus(Duration.ofMinutes(30)).plusSeconds(1));

    assertThat(limiter.tryAcquire("k").allowed()).isTrue();
  }

  @Test
  void windowSlides() {
    limiter.tryAcquire("k");
    now.set(now.get().plus(Duration.ofMinutes(40)));
    limiter.tryAcquire("k");
    limiter.tryAcquire("k");
    assertThat(limiter.tryAcquire("k").allowed()).isFalse();

    now.set(now.get().plus(Duration.ofMinutes(20)).plusSeconds(1));

    RateLimiter.Decision decision = limiter.tryAcquire("k");
    assertThat(decision.allowed()).isTrue();
    assertThat(limiter.tryAcquire("k").retryAfterSeconds())
        .isEqualTo(Duration.ofMinutes(40).toSeconds() - 1);
  }

  @Test
  void exactlyOneHourLaterTheOldHitHasExpired() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("k");
    }
    now.set(now.get().plus(Duration.ofHours(1)));

    assertThat(limiter.tryAcquire("k").allowed()).isTrue();
  }

  @Test
  void keysAreIndependent() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("a");
    }
    assertThat(limiter.tryAcquire("a").allowed()).isFalse();
    assertThat(limiter.tryAcquire("b").allowed()).isTrue();
  }

  @Test
  void hitsInTheFutureDoNotCountNow() {
    now.set(now.get().plus(Duration.ofMinutes(10)));
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("k");
    }
    now.set(now.get().minus(Duration.ofMinutes(5)));

    assertThat(limiter.tryAcquire("k").allowed()).isTrue();
  }

  @Test
  void checkAndRecordForFailedLogins() {
    assertThat(limiter.check("f").allowed()).isTrue();
    limiter.record("f");
    limiter.record("f");
    assertThat(limiter.check("f").allowed()).isTrue();
    limiter.record("f");

    RateLimiter.Decision blocked = limiter.check("f");

    assertThat(blocked.allowed()).isFalse();
    assertThat(blocked.retryAfterSeconds()).isPositive();
    now.set(now.get().plus(Duration.ofHours(1)).plusSeconds(1));
    assertThat(limiter.check("f").allowed()).isTrue();
  }

  @Test
  void retryAfterIsAtLeastOneSecond() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("k");
    }
    now.set(now.get().plus(Duration.ofHours(1)).minusMillis(1));

    assertThat(limiter.tryAcquire("k").retryAfterSeconds()).isEqualTo(1);
  }

  @Test
  void limitMustBePositive() {
    assertThatThrownBy(() -> new RateLimiter(0, now::get))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
