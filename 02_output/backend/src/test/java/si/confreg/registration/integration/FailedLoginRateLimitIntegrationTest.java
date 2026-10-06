package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

/** Failed organizer logins are rate limited per client (SB-06). Own context, small limit. */
@TestPropertySource(properties = "app.rate-limit-per-hour=3")
class FailedLoginRateLimitIntegrationTest extends IntegrationTestBase {

  @Value("${app.rate-limit-per-hour}")
  int limit;

  @Test
  void organizerEndpointsAreBlockedAfterTooManyFailedLogins() throws Exception {
    for (int i = 0; i < limit; i++) {
      assertThat(get("/api/registrations/CR-X", ORGANIZER, "wrong").statusCode()).isEqualTo(401);
    }

    assertThat(get("/api/registrations/CR-X", ORGANIZER, "wrong").statusCode()).isEqualTo(429);
    assertThat(get("/api/registrations/CR-X", ORGANIZER, PASSWORD).statusCode()).isEqualTo(429);
  }
}
