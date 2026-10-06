package si.confreg.registration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import si.confreg.registration.application.TestSettings;
import si.confreg.registration.infrastructure.web.RateLimiter.Bucket;

class RateLimiterTest {

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-08-01T10:00:00Z"));
  private final RateLimiter limiter = new RateLimiter(TestSettings.properties(), now::get);

  @Test
  void allowsTheConfiguredNumberPerHourThenAnswersRetryAfter() {
    for (int i = 0; i < 3; i++) {
      assertThat(limiter.acquire(Bucket.REGISTER, "1.2.3.4")).isZero();
    }
    now.set(Instant.parse("2026-08-01T10:20:00Z"));

    assertThat(limiter.acquire(Bucket.REGISTER, "1.2.3.4")).isEqualTo(40 * 60);
  }

  @Test
  void newWindowAfterOneHour() {
    for (int i = 0; i < 3; i++) {
      limiter.acquire(Bucket.REGISTER, "1.2.3.4");
    }
    now.set(Instant.parse("2026-08-01T11:00:00Z"));

    assertThat(limiter.acquire(Bucket.REGISTER, "1.2.3.4")).isZero();
  }

  @Test
  void clientsAndBucketsAreIndependent() {
    for (int i = 0; i < 3; i++) {
      limiter.acquire(Bucket.REGISTER, "1.2.3.4");
    }

    assertThat(limiter.acquire(Bucket.REGISTER, "5.6.7.8")).isZero();
    assertThat(limiter.acquire(Bucket.OPTIONS, "1.2.3.4")).isZero();
  }

  @Test
  void checkDoesNotCount() {
    for (int i = 0; i < 10; i++) {
      assertThat(limiter.check(Bucket.AUTH_FAILURE, "1.2.3.4")).isZero();
    }
    for (int i = 0; i < 3; i++) {
      limiter.acquire(Bucket.AUTH_FAILURE, "1.2.3.4");
    }

    assertThat(limiter.check(Bucket.AUTH_FAILURE, "1.2.3.4")).isPositive();
  }

  @Test
  void retryAfterIsAtLeastOneSecond() {
    for (int i = 0; i < 3; i++) {
      limiter.acquire(Bucket.REGISTER, "1.2.3.4");
    }
    now.set(Instant.parse("2026-08-01T10:59:59.900Z"));

    assertThat(limiter.acquire(Bucket.REGISTER, "1.2.3.4")).isEqualTo(1);
  }
}
