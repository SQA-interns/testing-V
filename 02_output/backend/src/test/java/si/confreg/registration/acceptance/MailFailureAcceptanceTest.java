package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * US-001 AC-001-09: when the SMTP server does not accept the confirmation e-mail, the request fails
 * without internal details and nothing is stored (D-25). SMTP points at a closed port.
 */
class MailFailureAcceptanceTest extends AcceptanceTestBase {

  private static final int CLOSED_PORT = closedPort();

  @DynamicPropertySource
  static void unreachableSmtp(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.port", () -> CLOSED_PORT);
    registry.add("SPRING_MAIL_PORT", () -> CLOSED_PORT);
    registry.add("spring.mail.properties.mail.smtp.connectiontimeout", () -> "2000");
    registry.add("spring.mail.properties.mail.smtp.timeout", () -> "2000");
    registry.add("spring.mail.properties.mail.smtp.starttls.required", () -> "true");
  }

  @Test
  void AC_001_09_registrationFailsAndIsNotStoredWhenEmailCannotBeSent() {
    String email = uniqueEmail("smtp-down");
    long before = storedRegistrations();

    HttpResponse<String> response =
        postRegistration(privateRegistration(email), deadlineDayStart());

    assertThat(response.statusCode()).as(response.body()).isBetween(500, 599);
    assertThat((String) json(response, "$.error")).isEqualTo("registration_unavailable");
    assertThat(storedRegistrationsFor(email)).isZero();
    assertThat(storedRegistrations()).isEqualTo(before);
  }

  @Test
  void AC_001_09_failureResponseHasNoInternalDetails() {
    HttpResponse<String> response =
        postRegistration(privateRegistration(uniqueEmail("smtp-down-details")), deadlineDayStart());

    assertThat(response.statusCode()).isBetween(500, 599);
    assertThat(response.body()).doesNotContainIgnoringCase("exception");
    assertThat(response.body()).doesNotContainIgnoringCase("smtp");
    assertThat(response.body()).doesNotContain(String.valueOf(CLOSED_PORT));
  }
}
