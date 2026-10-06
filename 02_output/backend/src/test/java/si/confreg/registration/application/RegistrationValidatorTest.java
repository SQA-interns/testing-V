package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration.NewRegistration;

class RegistrationValidatorTest {

  private final RegistrationValidator validator =
      new RegistrationValidator(TestSettings.properties());

  private static RegistrationCommand privateCommand(String email) {
    return new RegistrationCommand(
        " Ana ",
        " Novak ",
        " " + email + " ",
        "private",
        null,
        null,
        null,
        List.of(),
        false,
        Set.of());
  }

  private static RegistrationCommand company(String name, String address, String vat) {
    return new RegistrationCommand(
        "Ana", "Novak", "ana@example.org", "company", name, address, vat, null, false, Set.of());
  }

  private List<FieldError> errorsOf(RegistrationCommand command) {
    try {
      validator.validate(command);
    } catch (ValidationFailedException e) {
      return e.errors();
    }
    throw new AssertionError("expected rejection");
  }

  private static List<String> codes(List<FieldError> errors, String field) {
    return errors.stream().filter(e -> e.field().equals(field)).map(FieldError::code).toList();
  }

  @Test
  void validPrivateRegistrationIsTrimmed() {
    NewRegistration data = validator.validate(privateCommand("ana@example.org"));

    assertThat(data.firstName()).isEqualTo("Ana");
    assertThat(data.lastName()).isEqualTo("Novak");
    assertThat(data.email()).isEqualTo("ana@example.org");
    assertThat(data.payerType()).isEqualTo(PayerType.PRIVATE);
    assertThat(data.companyName()).isNull();
    assertThat(data.workshop()).isNull();
    assertThat(data.student()).isFalse();
  }

  @Test
  void validCompanyRegistrationKeepsAddressLineBreaks() {
    NewRegistration data =
        validator.validate(company(" Primer d.o.o. ", "Cesta 1\r\n1000 Ljubljana", " SI-123/4.5 "));

    assertThat(data.payerType()).isEqualTo(PayerType.COMPANY);
    assertThat(data.companyName()).isEqualTo("Primer d.o.o.");
    assertThat(data.companyAddress()).isEqualTo("Cesta 1\r\n1000 Ljubljana");
    assertThat(data.companyVatId()).isEqualTo("SI-123/4.5");
  }

  @Test
  void allRequiredMissingAreReportedTogether() {
    RegistrationCommand empty =
        new RegistrationCommand(null, "", "  ", null, null, null, null, null, false, Set.of());

    List<FieldError> errors = errorsOf(empty);

    assertThat(errors)
        .extracting(FieldError::field)
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "payerType");
    assertThat(errors).extracting(FieldError::code).containsOnly("required");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plain",
        "a@b",
        "@example.org",
        "a b@example.org",
        "a@b..org",
        "a@.org",
        "a@@b.org"
      })
  void invalidEmails(String email) {
    assertThat(codes(errorsOf(privateCommand(email)), "email")).containsExactly("invalid");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ana@example.org", "a.b+c@sub.example.co", "žiga@primer.si"})
  void validEmails(String email) {
    assertThat(validator.validate(privateCommand(email)).email()).isEqualTo(email);
  }

  @Test
  void emailLongerThan254IsTooLong() {
    String email = "a".repeat(243) + "@example.org"; // 255 characters

    assertThat(codes(errorsOf(privateCommand(email)), "email")).containsExactly("too_long");
  }

  @Test
  void namesAreLimitedByCharactersNotUtf16Units() {
    String hundredEmoji = "😀".repeat(100);
    RegistrationCommand command =
        new RegistrationCommand(
            hundredEmoji,
            "Novak",
            "ana@example.org",
            "private",
            null,
            null,
            null,
            null,
            false,
            Set.of());

    assertThat(validator.validate(command).firstName()).isEqualTo(hundredEmoji);
    RegistrationCommand tooLong =
        new RegistrationCommand(
            hundredEmoji + "a",
            "Novak",
            "ana@example.org",
            "private",
            null,
            null,
            null,
            null,
            false,
            Set.of());
    assertThat(codes(errorsOf(tooLong), "firstName")).containsExactly("too_long");
  }

  @Test
  void controlCharactersInNamesAreRejected() {
    RegistrationCommand command =
        new RegistrationCommand(
            "Ana\nBcc: x",
            "No\tvak",
            "ana@example.org",
            "private",
            null,
            null,
            null,
            null,
            false,
            Set.of());

    List<FieldError> errors = errorsOf(command);
    assertThat(codes(errors, "firstName")).containsExactly("invalid");
    assertThat(codes(errors, "lastName")).containsExactly("invalid");
  }

  @Test
  void companyAddressRejectsControlCharactersOtherThanLineBreaks() {
    assertThat(codes(errorsOf(company("Primer", "Cesta\u00071", null)), "companyAddress"))
        .containsExactly("invalid");
    assertThat(validator.validate(company("Primer\n", "Cesta 1", null)).companyName())
        .isEqualTo("Primer");
    assertThat(codes(errorsOf(company("Pri\nmer", "Cesta 1", null)), "companyName"))
        .containsExactly("invalid");
  }

  @Test
  void unknownPayerTypeIsInvalidAndSkipsCompanyRules() {
    RegistrationCommand command =
        new RegistrationCommand(
            "Ana", "Novak", "ana@example.org", "PRIVATE", "X", null, null, null, false, Set.of());

    List<FieldError> errors = errorsOf(command);
    assertThat(errors).extracting(FieldError::field).containsExactly("payerType");
    assertThat(codes(errors, "payerType")).containsExactly("invalid");
  }

  @Test
  void companyNeedsNameAndAddressButNotVatId() {
    List<FieldError> errors = errorsOf(company(" ", null, null));

    assertThat(codes(errors, "companyName")).containsExactly("required");
    assertThat(codes(errors, "companyAddress")).containsExactly("required");
    assertThat(codes(errors, "companyVatId")).isEmpty();
  }

  @Test
  void companyFieldLimits() {
    List<FieldError> errors = errorsOf(company("n".repeat(201), "a".repeat(501), "1".repeat(31)));

    assertThat(codes(errors, "companyName")).containsExactly("too_long");
    assertThat(codes(errors, "companyAddress")).containsExactly("too_long");
    assertThat(codes(errors, "companyVatId")).containsExactly("too_long");
  }

  @Test
  void vatIdWithForbiddenCharactersIsInvalid() {
    assertThat(codes(errorsOf(company("Primer", "Cesta 1", "SI<123>")), "companyVatId"))
        .containsExactly("invalid");
  }

  @Test
  void privatePayerMustNotSendCompanyData() {
    RegistrationCommand command =
        new RegistrationCommand(
            "Ana", "Novak", "ana@example.org", "private", "X", "Y", "Z", null, false, Set.of());

    List<FieldError> errors = errorsOf(command);
    assertThat(errors).extracting(FieldError::code).containsOnly("not_allowed");
    assertThat(errors)
        .extracting(FieldError::field)
        .containsExactly("companyName", "companyAddress", "companyVatId");
  }

  @Test
  void workshopRules() {
    assertThat(validator.validate(withWorkshops(List.of(" W2 "))).workshop()).isEqualTo("W2");
    assertThat(validator.validate(withWorkshops(null)).workshop()).isNull();
    assertThat(codes(errorsOf(withWorkshops(List.of("W1", "W2"))), "workshops"))
        .containsExactly("too_many");
    assertThat(codes(errorsOf(withWorkshops(List.of("W9"))), "workshops"))
        .containsExactly("invalid");
  }

  @Test
  void typeErrorsAreReportedOnceAsInvalid() {
    RegistrationCommand command =
        new RegistrationCommand(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            Set.of("firstName", "lastName", "email", "payerType", "workshops", "student"));

    List<FieldError> errors = errorsOf(command);

    assertThat(errors).extracting(FieldError::code).containsOnly("invalid");
    assertThat(errors)
        .extracting(FieldError::field)
        .containsExactly("email", "firstName", "lastName", "payerType", "student", "workshops");
  }

  @Test
  void studentFlagIsKept() {
    RegistrationCommand command =
        new RegistrationCommand(
            "Ana", "Novak", "ana@example.org", "private", null, null, null, null, true, Set.of());

    assertThat(validator.validate(command).student()).isTrue();
  }

  @Test
  void rejectionCarriesAllErrors() {
    assertThatThrownBy(() -> validator.validate(privateCommand("bad")))
        .isInstanceOf(ValidationFailedException.class)
        .hasMessage("registration rejected");
  }

  private static RegistrationCommand withWorkshops(List<String> workshops) {
    return new RegistrationCommand(
        "Ana", "Novak", "ana@example.org", "private", null, null, null, workshops, false, Set.of());
  }
}
