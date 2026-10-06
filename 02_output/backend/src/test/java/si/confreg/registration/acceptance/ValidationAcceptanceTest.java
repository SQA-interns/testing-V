package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** US-001 AC-001-08: invalid registrations are rejected; nothing stored, no e-mail (D-23). */
class ValidationAcceptanceTest extends AcceptanceTestBase {

  private static Arguments invalid(
      String description, String field, boolean company, Consumer<Map<String, Object>> change) {
    return Arguments.of(description, field, company, change);
  }

  static Stream<Arguments> invalidRegistrations() {
    return Stream.of(
        invalid("missing first name", "firstName", false, b -> b.remove("firstName")),
        invalid("blank last name", "lastName", false, b -> b.put("lastName", "   ")),
        invalid(
            "first name too long", "firstName", false, b -> b.put("firstName", "A".repeat(101))),
        invalid("missing e-mail", "email", false, b -> b.remove("email")),
        invalid("malformed e-mail", "email", false, b -> b.put("email", "not-an-address")),
        invalid(
            "e-mail with header injection",
            "email",
            false,
            b -> b.put("email", b.get("email") + "\r\nBcc: victim@example.com")),
        invalid("missing payer type", "payerType", false, b -> b.remove("payerType")),
        invalid("unknown payer type", "payerType", false, b -> b.put("payerType", "government")),
        invalid("company without VAT ID", "companyVatId", true, b -> b.put("companyVatId", null)),
        invalid("company without name", "companyName", true, b -> b.remove("companyName")),
        invalid(
            "company without address", "companyAddress", true, b -> b.put("companyAddress", "")),
        invalid(
            "private payer with company name",
            "companyName",
            false,
            b -> b.put("companyName", "X d.o.o.")),
        invalid("two workshops", "workshops", false, b -> b.put("workshops", workshopIds())),
        invalid("unknown workshop", "workshops", false, b -> b.put("workshops", List.of("NOPE"))),
        invalid("unknown property", "discountCode", false, b -> b.put("discountCode", "FREE")));
  }

  @ParameterizedTest(name = "AC-001-08 rejects {0}")
  @MethodSource("invalidRegistrations")
  void AC_001_08_invalidRegistrationIsRejectedAndNothingStoredOrSent(
      String description, String field, boolean company, Consumer<Map<String, Object>> change) {
    String email = uniqueEmail("invalid");
    Map<String, Object> body = company ? companyRegistration(email) : privateRegistration(email);
    change.accept(body);
    long before = storedRegistrations();

    HttpResponse<String> response = postRegistration(body, deadlineDayStart());

    assertThat(response.statusCode()).as(description + ": " + response.body()).isEqualTo(400);
    assertThat((String) json(response, "$.error")).isEqualTo("validation_failed");
    List<String> fields =
        json(response, "$.fields") instanceof List<?> l
            ? l.stream().map(String::valueOf).toList()
            : List.of();
    assertThat(fields).as(description).contains(field);
    assertNothingStoredOrSent(before, email);
  }

  @Test
  void AC_001_08_malformedJsonIsRejectedAndNothingStored() {
    long before = storedRegistrations();

    HttpResponse<String> response = postRaw("{\"firstName\": \"Ana\", ", deadlineDayStart());

    assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
    assertThat(storedRegistrations()).isEqualTo(before);
  }

  @Test
  void AC_001_08_rejectionDoesNotExposeInternals() {
    Map<String, Object> body = privateRegistration(uniqueEmail("invalid-internals"));
    body.put("email", "not-an-address");

    HttpResponse<String> response = postRegistration(body, deadlineDayStart());

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).doesNotContainIgnoringCase("exception");
    assertThat(response.body()).doesNotContain("at si.confreg");
    assertThat(response.body()).doesNotContainIgnoringCase("org.springframework");
  }

  private static void assertNothingStoredOrSent(long before, String email) {
    assertThat(storedRegistrations()).isEqualTo(before);
    assertThat(storedRegistrationsFor(email)).isZero();
    String plainAddress = email.split("\r", 2)[0];
    await()
        .during(Duration.ofSeconds(1))
        .atMost(Duration.ofSeconds(3))
        .until(() -> mailIdsTo(plainAddress).isEmpty());
  }
}
