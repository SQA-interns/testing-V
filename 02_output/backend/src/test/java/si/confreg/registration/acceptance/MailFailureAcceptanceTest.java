package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.ObjectNode;

/** US-001 AC3 failure case: AC-001-18 (D-22). The backend's SMTP port has nothing listening. */
class MailFailureAcceptanceTest extends AcceptanceTestBase {

  private static final int CLOSED_SMTP_PORT = closedPort();

  @DynamicPropertySource
  static void unreachableSmtp(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.host", () -> "127.0.0.1");
    registry.add("spring.mail.port", () -> CLOSED_SMTP_PORT);
  }

  @Test
  void ac_001_18_registrationNotStoredWhenEmailCannotBeSent() {
    ObjectNode body = privateRegistration();
    long before = storedRegistrations();

    HttpResponse<String> response = register(body, lastEarlyInstant());

    assertThat(response.statusCode()).as("status, body: %s", response.body()).isBetween(500, 599);
    assertThat(response.body()).containsIgnoringCase("try again");
    assertThat(storedRegistrations()).isEqualTo(before);
  }

  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
