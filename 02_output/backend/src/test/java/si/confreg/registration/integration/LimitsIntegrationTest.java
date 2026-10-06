package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/** SB-06 rate limit and SR-04 disabled test clock, in a context with a small limit. */
@TestPropertySource(
    properties = {
      "app.rate-limit-per-hour=3",
      "app.test-clock=disabled",
      "app.early-bird-deadline=2000-01-01"
    })
class LimitsIntegrationTest extends IntegrationTestBase {

  @Autowired Environment environment;

  @Test
  void testClockHeaderIgnoredWhenDisabledAndRateLimitApplies() {
    String body =
        """
        {"firstName":"Ana","lastName":"Kovač","email":"limits@example.com","payerType":"private"}
        """;
    HttpRequest withPastInstant =
        HttpRequest.newBuilder(uri("/api/registrations"))
            .header("Content-Type", "application/json")
            .header("X-Test-Now", "1999-01-01T00:00:00Z")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> first = send(withPastInstant);

    assertThat(first.statusCode()).isEqualTo(201);
    assertThat(json(first).get("grossFee").decimalValue())
        .isEqualByComparingTo(new BigDecimal(environment.getRequiredProperty("app.fee-regular")));

    assertThat(get("/api/registrations/REG-000000", USERNAME, "wrong").statusCode()).isEqualTo(401);
    assertThat(send(withPastInstant).statusCode()).isEqualTo(201);
    HttpResponse<String> limited = send(withPastInstant);
    assertThat(limited.statusCode()).isEqualTo(429);
    assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    assertThat(get("/api/registrations/REG-000000", USERNAME, PASSWORD).statusCode())
        .isEqualTo(429);
    assertThat(get("/actuator/health", null, null).statusCode()).isEqualTo(200);
  }
}
