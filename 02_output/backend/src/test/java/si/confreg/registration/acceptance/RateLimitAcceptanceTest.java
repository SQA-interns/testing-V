package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Rate limit on public registration (US-001: AC-001-13; D-11). Runs in its own application context
 * with a small configured limit, read back from the configuration.
 */
@TestPropertySource(properties = "app.rate-limit-per-hour=3")
class RateLimitAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_13_requestOverTheHourlyLimitIsRejectedNothingStoredNoMail() {
    int limit = rateLimitPerHour();
    for (int i = 0; i < limit; i++) {
      register(privatePayer(uniqueEmail()), earlyInstant());
    }
    String rejectedEmail = uniqueEmail();

    ApiResponse response = postRegistration(privatePayer(rejectedEmail), earlyInstant());

    assertThat(response.status()).as("status of %s", response.body()).isEqualTo(429);
    assertThat(response.raw().headers().firstValue("Retry-After")).isPresent();
    assertThat(storedRegistrationsFor(rejectedEmail)).as("nothing stored").isZero();
    assertNoMailTo(rejectedEmail);
  }
}
