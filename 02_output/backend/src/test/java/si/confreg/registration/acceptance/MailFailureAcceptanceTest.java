package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * US-001 AC3 failure case: AC-001-18 (D-22). The shared Mailpit container is paused during the
 * request, so the backend's SMTP server does not answer (D-34).
 */
class MailFailureAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_18_registrationNotStoredWhenEmailCannotBeSent() {
    ObjectNode body = privateRegistration();
    long before = storedRegistrations();

    HttpResponse<String> response;
    MAILPIT.getDockerClient().pauseContainerCmd(MAILPIT.getContainerId()).exec();
    try {
      response = register(body, lastEarlyInstant());
    } finally {
      MAILPIT.getDockerClient().unpauseContainerCmd(MAILPIT.getContainerId()).exec();
    }

    assertThat(response.statusCode()).as("status, body: %s", response.body()).isBetween(500, 599);
    assertThat(response.body()).containsIgnoringCase("try again");
    assertThat(storedRegistrations()).isEqualTo(before);
  }
}
