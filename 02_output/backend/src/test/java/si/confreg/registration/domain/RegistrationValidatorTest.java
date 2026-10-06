package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.confreg.registration.testsupport.TestSettings;

class RegistrationValidatorTest {

  private final RegistrationValidator validator =
      new RegistrationValidator(TestSettings.settings());

  private static Map<String, Object> privateBody() {
    Map<String, Object> body = new HashMap<>();
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", "ana.novak@example.org");
    body.put("payerType", "private");
    return body;
  }

  private static Map<String, Object> companyBody() {
    Map<String, Object> body = privateBody();
    body.put("payerType", "company");
    body.put("companyName", "Primer d.o.o.");
    body.put("companyAddress", "Koroška cesta 1, 2000 Maribor");
    body.put("companyVatId", "SI00000001");
    return body;
  }

  private List<FieldError> errorsOf(Map<String, Object> body) {
    try {
      validator.validate(body);
    } catch (ValidationFailedException e) {
      return e.errors();
    }
    throw new AssertionError("expected a validation failure");
  }

  private List<String> fieldsOf(Map<String, Object> body) {
    return errorsOf(body).stream().map(FieldError::field).toList();
  }

  @Test
  void validPrivateBodyIsTrimmedAndHasNoCompanyData() {
    Map<String, Object> body = privateBody();
    body.put("firstName", "  Ana ");
    body.put("companyName", "ignored");
    RegistrationData data = validator.validate(body);
    assertThat(data.firstName()).isEqualTo("Ana");
    assertThat(data.payerType()).isEqualTo(PayerType.PRIVATE);
    assertThat(data.companyName()).isNull();
    assertThat(data.companyAddress()).isNull();
    assertThat(data.companyVatId()).isNull();
    assertThat(data.workshop()).isNull();
  }

  @Test
  void validCompanyBodyKeepsSlovenianCharacters() {
    RegistrationData data = validator.validate(companyBody());
    assertThat(data.payerType()).isEqualTo(PayerType.COMPANY);
    assertThat(data.companyAddress()).isEqualTo("Koroška cesta 1, 2000 Maribor");
  }

  @Test
  void configuredWorkshopIsAccepted() {
    Map<String, Object> body = privateBody();
    body.put("workshops", List.of("WB"));
    assertThat(validator.validate(body).workshop()).isEqualTo("WB");
  }

  @Test
  void emptyOrNullWorkshopsMeanNone() {
    Map<String, Object> body = privateBody();
    body.put("workshops", List.of());
    assertThat(validator.validate(body).workshop()).isNull();
    body.put("workshops", null);
    assertThat(validator.validate(body).workshop()).isNull();
  }

  @Test
  void workshopsMustBeAList() {
    Map<String, Object> body = privateBody();
    body.put("workshops", "WA");
    assertThat(fieldsOf(body)).containsExactly("workshops");
  }

  @Test
  void workshopIdMustBeText() {
    Map<String, Object> body = privateBody();
    body.put("workshops", List.of(1));
    assertThat(errorsOf(body)).containsExactly(new FieldError("workshops", "unknown workshop"));
  }

  @Test
  void moreThanOneWorkshopIsRejected() {
    Map<String, Object> body = privateBody();
    body.put("workshops", List.of("WA", "WA"));
    assertThat(errorsOf(body))
        .containsExactly(new FieldError("workshops", "at most one workshop may be selected"));
  }

  @Test
  void blankFirstNameIsRequired() {
    Map<String, Object> body = privateBody();
    body.put("firstName", " 	 ");
    assertThat(errorsOf(body)).containsExactly(new FieldError("firstName", "is required"));
  }

  @Test
  void nonTextNameIsRejected() {
    Map<String, Object> body = privateBody();
    body.put("firstName", 42);
    assertThat(errorsOf(body)).containsExactly(new FieldError("firstName", "must be text"));
  }

  @Test
  void controlCharacterInNameIsRejected() {
    Map<String, Object> body = privateBody();
    body.put("lastName", "No\u0000vak");
    assertThat(errorsOf(body))
        .containsExactly(new FieldError("lastName", "must not contain control characters"));
  }

  @Test
  void maximumLengthsAreAcceptedAndOneMoreIsRejected() {
    Map<String, Object> body = companyBody();
    body.put("firstName", "a".repeat(RegistrationValidator.NAME_MAX));
    body.put("companyName", "c".repeat(RegistrationValidator.COMPANY_NAME_MAX));
    body.put("companyAddress", "d".repeat(RegistrationValidator.COMPANY_ADDRESS_MAX));
    body.put("companyVatId", "v".repeat(RegistrationValidator.VAT_ID_MAX));
    validator.validate(body);

    body.put("firstName", "a".repeat(RegistrationValidator.NAME_MAX + 1));
    body.put("companyVatId", "v".repeat(RegistrationValidator.VAT_ID_MAX + 1));
    assertThat(errorsOf(body))
        .containsExactly(
            new FieldError("firstName", "must be at most 100 characters"),
            new FieldError("companyVatId", "must be at most 30 characters"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@example.org",
        "ana@example",
        "ana@@example.org",
        "ana novak@example.org",
        "ana@example.",
        "ana..novak@example.org",
        "ana@example..org"
      })
  void invalidEmailsAreRejected(String email) {
    Map<String, Object> body = privateBody();
    body.put("email", email);
    assertThat(errorsOf(body))
        .containsExactly(new FieldError("email", "must be a valid e-mail address"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a@b.si", "ana.novak+tag@example.org", "x@sub.example.co.uk"})
  void validEmailsAreAccepted(String email) {
    Map<String, Object> body = privateBody();
    body.put("email", email);
    assertThat(validator.validate(body).email()).isEqualTo(email);
  }

  @Test
  void emailLocalPartLongerThan64IsRejectedAnd64IsAccepted() {
    Map<String, Object> body = privateBody();
    body.put("email", "l".repeat(64) + "@example.org");
    validator.validate(body);
    body.put("email", "l".repeat(65) + "@example.org");
    assertThat(fieldsOf(body)).containsExactly("email");
  }

  @Test
  void emailLongerThan254IsRejected() {
    Map<String, Object> body = privateBody();
    body.put("email", "a@" + "d".repeat(250) + ".org");
    assertThat(errorsOf(body))
        .containsExactly(new FieldError("email", "must be at most 254 characters"));
  }

  @Test
  void blankPayerTypeIsRequiredAndNonTextIsInvalid() {
    Map<String, Object> body = privateBody();
    body.put("payerType", " ");
    assertThat(errorsOf(body)).containsExactly(new FieldError("payerType", "is required"));
    body.put("payerType", 1);
    assertThat(errorsOf(body))
        .containsExactly(new FieldError("payerType", "must be private or company"));
  }

  @Test
  void payerTypeIsCaseSensitive() {
    Map<String, Object> body = privateBody();
    body.put("payerType", "Company");
    assertThat(fieldsOf(body)).containsExactly("payerType");
  }

  @Test
  void errorsAreInFieldOrderAndOnePerField() {
    Map<String, Object> body = new HashMap<>();
    body.put("payerType", "company");
    body.put("workshops", List.of("unknown"));
    assertThat(fieldsOf(body))
        .containsExactly(
            "firstName",
            "lastName",
            "email",
            "companyName",
            "companyAddress",
            "companyVatId",
            "workshops");
  }

  @Test
  void exceptionMessageContainsNoPersonalData() {
    Map<String, Object> body = privateBody();
    body.put("email", "ana.novak@invalid");
    assertThatThrownBy(() -> validator.validate(body))
        .isInstanceOf(ValidationFailedException.class)
        .hasMessageNotContaining("ana.novak");
  }
}
