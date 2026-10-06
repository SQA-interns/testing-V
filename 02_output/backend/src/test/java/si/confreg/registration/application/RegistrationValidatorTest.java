package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Workshop;
import si.confreg.registration.domain.WorkshopCatalog;

/** Unit tests of the field rules (AC-001-07, D-08). Workshops here are synthetic test data. */
class RegistrationValidatorTest {

  private final RegistrationValidator validator =
      new RegistrationValidator(
          new WorkshopCatalog(List.of(new Workshop("T1", "Test one"), new Workshop("T2", "Two"))));

  private static RegistrationRequest request(
      String first,
      String last,
      String email,
      String payerType,
      String name,
      String address,
      String vat,
      List<String> workshops,
      String... wrongType) {
    return new RegistrationRequest(
        first, last, email, payerType, name, address, vat, workshops, Set.of(wrongType));
  }

  private static RegistrationRequest validPrivate() {
    return request("Ana", "Novak", "ana@example.org", "private", null, null, null, List.of());
  }

  @Test
  void validPrivateRequestIsNormalisedAndTrimmed() {
    RegistrationValidator.Result result =
        validator.validate(
            request(
                "  Ana ",
                "\tNovak",
                " ana@example.org ",
                "private",
                "ignored",
                "ignored",
                "ignored",
                List.of("T2")));

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
    assertThat(result.valid().firstName()).isEqualTo("Ana");
    assertThat(result.valid().lastName()).isEqualTo("Novak");
    assertThat(result.valid().email()).isEqualTo("ana@example.org");
    assertThat(result.valid().payer().type()).isEqualTo(PayerType.PRIVATE);
    assertThat(result.valid().payer().companyName()).isNull();
    assertThat(result.valid().workshopId()).isEqualTo("T2");
  }

  @Test
  void validCompanyRequestKeepsCompanyData() {
    RegistrationValidator.Result result =
        validator.validate(
            request("A", "B", "a@b.co", "company", " Firma ", " Cesta 1 ", " SI1 ", null));

    assertThat(result.isValid()).isTrue();
    assertThat(result.valid().payer().companyName()).isEqualTo("Firma");
    assertThat(result.valid().payer().companyAddress()).isEqualTo("Cesta 1");
    assertThat(result.valid().payer().companyVatId()).isEqualTo("SI1");
    assertThat(result.valid().workshopId()).isNull();
  }

  @Test
  void missingParticipantFieldsAreRequired() {
    RegistrationValidator.Result result =
        validator.validate(request(null, " ", "", null, null, null, null, null));

    assertThat(result.isValid()).isFalse();
    assertThat(result.valid()).isNull();
    assertThat(result.errors())
        .containsEntry("firstName", RegistrationValidator.REQUIRED)
        .containsEntry("lastName", RegistrationValidator.REQUIRED)
        .containsEntry("email", RegistrationValidator.REQUIRED)
        .containsEntry("payerType", RegistrationValidator.REQUIRED)
        .hasSize(4);
  }

  @Test
  void lengthLimitsAreInclusiveAndCountCharacters() {
    String max = "Ž".repeat(RegistrationValidator.NAME_MAX);
    assertThat(
            validator
                .validate(request(max, max, "a@b.co", "private", null, null, null, null))
                .isValid())
        .isTrue();

    RegistrationValidator.Result tooLong =
        validator.validate(request(max + "x", "B", "a@b.co", "private", null, null, null, null));

    assertThat(tooLong.errors()).containsOnlyKeys("firstName");
    assertThat(tooLong.errors().get("firstName")).contains("100");
  }

  @Test
  void emailLengthLimit() {
    String local = "a".repeat(RegistrationValidator.EMAIL_MAX - "@b.co".length());
    assertThat(
            validator
                .validate(request("A", "B", local + "@b.co", "private", null, null, null, null))
                .isValid())
        .isTrue();
    assertThat(
            validator
                .validate(request("A", "B", local + "a@b.co", "private", null, null, null, null))
                .errors())
        .containsOnlyKeys("email");
  }

  @ParameterizedTest
  @ValueSource(strings = {"plain", "a@b", "@b.co", "a@.", "a b@c.de", "a@b@c.de", "a@b.co x"})
  void invalidEmailFormats(String email) {
    assertThat(
            validator
                .validate(request("A", "B", email, "private", null, null, null, null))
                .errors())
        .containsEntry("email", RegistrationValidator.INVALID_EMAIL);
  }

  @ParameterizedTest
  @ValueSource(strings = {"No\nvak", "No\rvak", "No\u0000vak", "No\tvak", "No\u007Fvak"})
  void controlCharactersAreRejected(String value) {
    assertThat(
            validator
                .validate(request("A", value, "a@b.co", "private", null, null, null, null))
                .errors())
        .containsEntry("lastName", RegistrationValidator.CONTROL_CHARACTERS);
  }

  @Test
  void payerTypeMustBeExact() {
    assertThat(
            validator
                .validate(request("A", "B", "a@b.co", "Private", null, null, null, null))
                .errors())
        .containsEntry("payerType", RegistrationValidator.INVALID_PAYER_TYPE)
        .hasSize(1);
  }

  @Test
  void companyFieldsAreRequiredOnlyForCompanies() {
    RegistrationValidator.Result result =
        validator.validate(request("A", "B", "a@b.co", "company", "", null, "  ", null));

    assertThat(result.errors())
        .containsEntry("companyName", RegistrationValidator.REQUIRED_FOR_COMPANY)
        .containsEntry("companyAddress", RegistrationValidator.REQUIRED_FOR_COMPANY)
        .containsEntry("companyVatId", RegistrationValidator.REQUIRED_FOR_COMPANY)
        .hasSize(3);
  }

  @Test
  void companyFieldLimits() {
    RegistrationValidator.Result result =
        validator.validate(
            request(
                "A",
                "B",
                "a@b.co",
                "company",
                "n".repeat(RegistrationValidator.COMPANY_NAME_MAX + 1),
                "a".repeat(RegistrationValidator.COMPANY_ADDRESS_MAX + 1),
                "v".repeat(RegistrationValidator.VAT_ID_MAX + 1),
                null));

    assertThat(result.errors()).containsOnlyKeys("companyName", "companyAddress", "companyVatId");
  }

  @Test
  void workshopSelectionRules() {
    assertThat(errorsFor(List.of("T1", "T2")))
        .containsEntry("workshops", "select at most one workshop");
    assertThat(errorsFor(List.of("T1", "T1"))).containsKey("workshops");
    assertThat(errorsFor(List.of("t1"))).containsEntry("workshops", "is not an offered workshop");
    assertThat(errorsFor(new ArrayList<>(Arrays.asList((String) null)))).containsKey("workshops");
    assertThat(errorsFor(List.of())).isEmpty();
    assertThat(errorsFor(null)).isEmpty();
  }

  private java.util.Map<String, String> errorsFor(List<String> workshops) {
    return validator
        .validate(request("A", "B", "a@b.co", "private", null, null, null, workshops))
        .errors();
  }

  @Test
  void wrongJsonTypesAreReportedPerField() {
    RegistrationValidator.Result result =
        validator.validate(
            request(
                null,
                "B",
                null,
                null,
                null,
                null,
                null,
                null,
                "firstName",
                "email",
                "payerType",
                "workshops"));

    assertThat(result.errors())
        .containsEntry("firstName", RegistrationValidator.WRONG_TYPE)
        .containsEntry("email", RegistrationValidator.WRONG_TYPE)
        .containsEntry("payerType", RegistrationValidator.WRONG_TYPE)
        .containsEntry("workshops", RegistrationValidator.WRONG_TYPE)
        .hasSize(4);
  }

  @Test
  void wrongTypeInCompanyFieldIsReported() {
    assertThat(
            validator
                .validate(
                    request("A", "B", "a@b.co", "company", null, "x", "y", null, "companyName"))
                .errors())
        .containsEntry("companyName", RegistrationValidator.WRONG_TYPE)
        .hasSize(1);
  }

  @Test
  void errorsAreImmutable() {
    RegistrationValidator.Result result = validator.validate(validPrivate());
    assertThat(result.errors()).isEmpty();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> result.errors().put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
