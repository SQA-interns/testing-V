package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

/** AC-001-07 (API level): 422 with one error per invalid field, nothing stored, no e-mail. */
class ValidationAcceptanceTest extends AcceptanceTestBase {

  static Stream<Arguments> invalidRequests() {
    return Stream.of(
        invalid("firstName missing", b -> b.remove("firstName"), "firstName"),
        invalid("firstName blank", b -> b.put("firstName", "   "), "firstName"),
        invalid("firstName too long (D-08)", b -> b.put("firstName", "A".repeat(101)), "firstName"),
        invalid("lastName missing", b -> b.remove("lastName"), "lastName"),
        invalid("lastName with line break (D-08)", b -> b.put("lastName", "No\nvak"), "lastName"),
        invalid("email missing", b -> b.remove("email"), "email"),
        invalid("email without @", b -> b.put("email", "ana.novak.example.org"), "email"),
        invalid("email without domain dot", b -> b.put("email", "ana@example"), "email"),
        invalid(
            "email header injection",
            b -> b.put("email", "ana@example.org\r\nBcc: x@y.org"),
            "email"),
        invalid("payerType missing", b -> b.remove("payerType"), "payerType"),
        invalid("payerType unknown", b -> b.put("payerType", "student"), "payerType"),
        invalid(
            "company without companyName",
            b -> {
              b.putAll(companyFields());
              b.remove("companyName");
            },
            "companyName"),
        invalid(
            "company with blank companyAddress",
            b -> {
              b.putAll(companyFields());
              b.put("companyAddress", " ");
            },
            "companyAddress"),
        invalid(
            "company without companyVatId",
            b -> {
              b.putAll(companyFields());
              b.put("companyVatId", null);
            },
            "companyVatId"),
        invalid(
            "company without any company field",
            b -> b.put("payerType", "company"),
            "companyName",
            "companyAddress",
            "companyVatId"),
        invalid("workshops not an array", b -> b.put("workshops", "W"), "workshops"),
        invalid(
            "several invalid fields at once",
            b -> {
              b.remove("firstName");
              b.put("email", "nope");
              b.put("payerType", "");
            },
            "firstName",
            "email",
            "payerType"));
  }

  private static Map<String, Object> companyFields() {
    return Map.of(
        "payerType", "company",
        "companyName", "Primer d.o.o.",
        "companyAddress", "Koroška cesta 1, 2000 Maribor",
        "companyVatId", "SI00000001");
  }

  private static Arguments invalid(
      String description, Consumer<Map<String, Object>> change, String... fields) {
    return Arguments.of(description, change, Set.of(fields));
  }

  @ParameterizedTest(name = "AC-001-07 {0}")
  @MethodSource("invalidRequests")
  void ac001_07_invalidRequestIsRejected(
      String description, Consumer<Map<String, Object>> change, Set<String> expectedFields) {
    Map<String, Object> body = anaPrivate();
    change.accept(body);

    assertRejected(body, expectedFields);
  }

  @Test
  @DisplayName("AC-001-07 more than one workshop selected")
  void ac001_07_moreThanOneWorkshopIsRejected() {
    List<String> ids = workshopIds();
    assertThat(ids).as("configured workshops").hasSizeGreaterThanOrEqualTo(2);
    Map<String, Object> body = anaPrivate();
    body.put("workshops", List.of(ids.get(0), ids.get(1)));

    assertRejected(body, Set.of("workshops"));
  }

  @Test
  @DisplayName("AC-001-07 the same workshop selected twice counts as more than one")
  void ac001_07_duplicateWorkshopIsRejected() {
    String id = workshopIds().get(0);
    Map<String, Object> body = anaPrivate();
    body.put("workshops", List.of(id, id));

    assertRejected(body, Set.of("workshops"));
  }

  @Test
  @DisplayName("AC-001-07 a workshop id that is not configured (D-08)")
  void ac001_07_unknownWorkshopIsRejected() {
    String unknown = String.join("-", workshopIds()) + "-unknown";
    Map<String, Object> body = anaPrivate();
    body.put("workshops", List.of(unknown));

    assertRejected(body, Set.of("workshops"));
  }

  @Test
  @DisplayName("AC-001-07 empty JSON object reports every required participant field")
  void ac001_07_emptyObjectReportsEachRequiredField() {
    long before = storedRegistrationCount();

    ApiResponse response = postRegistration("{}", daysFromDeadline(-3), clientAddress);

    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(422);
    assertThat(errorFields(response))
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "payerType");
    assertNothingStoredOrSent(before);
  }

  private void assertRejected(Map<String, Object> body, Set<String> expectedFields) {
    long before = storedRegistrationCount();
    ApiResponse response = register(body, daysFromDeadline(-3));
    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(422);
    assertThat(errorFields(response)).containsExactlyInAnyOrderElementsOf(expectedFields);
    assertThat(response.body()).doesNotContain("registrationNumber");
    assertNothingStoredOrSent(before);
  }

  private void assertNothingStoredOrSent(long countBefore) {
    assertThat(storedRegistrationCount()).as("stored registrations").isEqualTo(countBefore);
    assertThat(totalMessagesAfterSettle()).as("e-mails sent").isZero();
  }

  /** Field names of the 422 problem body: one entry per invalid field. */
  private static List<String> errorFields(ApiResponse response) {
    JsonNode errors = response.json().path("errors");
    assertThat(errors.isObject()).as("errors object in %s", response.body()).isTrue();
    List<String> fields = new ArrayList<>();
    for (String field : errors.propertyNames()) {
      assertThat(errors.path(field).asString("")).as("message for %s", field).isNotBlank();
      fields.add(field);
    }
    return fields;
  }
}
