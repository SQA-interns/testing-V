package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AC-001-12: per-client rate limit APP_RATE_LIMIT_PER_HOUR on the public registration. */
class RateLimitAcceptanceTest extends AcceptanceTestBase {

  /** Uses up the client's budget with cheap invalid requests (each one counts, spec 7.4). */
  private void exhaustBudget(String client, Instant now) {
    int limit = rateLimitPerHour();
    for (int i = 0; i < limit; i++) {
      ApiResponse response = postRegistration("{}", now, client);
      assertThat(response.status()).as("request %d of %d", i + 1, limit).isEqualTo(422);
    }
  }

  @Test
  @DisplayName("AC-001-12 request over the hourly limit gets 429, nothing stored, no e-mail")
  void ac001_12_overLimitIsRejected() {
    Instant now = daysFromDeadline(-6);
    exhaustBudget(clientAddress, now);
    long before = storedRegistrationCount();

    ApiResponse response = register(anaPrivate(), now.plusSeconds(60));

    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(429);
    assertThat(response.header("Retry-After")).as("Retry-After header").isNotBlank();
    assertThat(response.body()).doesNotContain("registrationNumber");
    assertThat(storedRegistrationCount()).as("stored registrations").isEqualTo(before);
    assertThat(totalMessagesAfterSettle()).as("e-mails sent").isZero();
  }

  @Test
  @DisplayName("AC-001-12 other clients are not affected by one client's limit")
  void ac001_12_otherClientsUnaffected() {
    Instant now = daysFromDeadline(-6);
    exhaustBudget(clientAddress, now);

    clientAddress = randomClientAddress();
    ApiResponse other = register(anaPrivate(), now.plusSeconds(60));

    assertThat(other.status()).as("status, body: %s", other.body()).isEqualTo(201);
  }

  @Test
  @DisplayName("AC-001-12 the limit applies within one hour; later the client may register again")
  void ac001_12_limitWindowIsOneHour() {
    Instant now = daysFromDeadline(-6);
    exhaustBudget(clientAddress, now);
    assertThat(register(anaPrivate(), now.plus(Duration.ofMinutes(59))).status()).isEqualTo(429);

    ApiResponse later = register(anaPrivate(), now.plus(Duration.ofMinutes(61)));

    assertThat(later.status()).as("status, body: %s", later.body()).isEqualTo(201);
  }
}
