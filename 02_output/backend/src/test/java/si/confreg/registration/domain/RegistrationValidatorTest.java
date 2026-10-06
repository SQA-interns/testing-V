package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator =
      new RegistrationValidator(WorkshopCatalogue.parse("W1=One;W2=Two"));

  private static RegistrationInput privateInput() {
    return new RegistrationInput(
        "Špela", "Žagar Čeč", "spela@example.com", "private", null, null, null, null);
  }

  private static RegistrationInput companyInput() {
    return new RegistrationInput(
        "Matej",
        "Šuštar",
        "matej@example.com",
        "company",
        "Podjetje d.o.o.",
        "Šmartinska 1\n1000 Ljubljana",
        "SI12345678",
        List.of("W1"));
  }

  private static RegistrationInput withFirstName(String name) {
    RegistrationInput i = privateInput();
    return new RegistrationInput(
        name, i.lastName(), i.email(), i.payerType(), null, null, null, null);
  }

  private static RegistrationInput withEmail(String email) {
    RegistrationInput i = privateInput();
    return new RegistrationInput(
        i.firstName(), i.lastName(), email, i.payerType(), null, null, null, null);
  }

  private static RegistrationInput withWorkshops(List<String> workshops) {
    RegistrationInput i = privateInput();
    return new RegistrationInput(
        i.firstName(), i.lastName(), i.email(), i.payerType(), null, null, null, workshops);
  }

  @Test
  void acceptsPrivatePayerAndKeepsSlovenianCharacters() {
    RegistrationValidator.Result result = validator.validate(privateInput());

    assertThat(result.valid()).isTrue();
    assertThat(result.participant().firstName()).isEqualTo("Špela");
    assertThat(result.participant().lastName()).isEqualTo("Žagar Čeč");
    assertThat(result.participant().payerType()).isEqualTo(PayerType.PRIVATE);
    assertThat(result.participant().company()).isNull();
    assertThat(result.participant().workshopId()).isNull();
  }

  @Test
  void acceptsCompanyPayerWithMultiLineAddressAndWorkshop() {
    RegistrationValidator.Result result = validator.validate(companyInput());

    assertThat(result.valid()).as(String.valueOf(result.invalidFields())).isTrue();
    assertThat(result.participant().company())
        .isEqualTo(new Company("Podjetje d.o.o.", "Šmartinska 1\n1000 Ljubljana", "SI12345678"));
    assertThat(result.participant().workshopId()).isEqualTo("W1");
  }

  @Test
  void trimsValues() {
    RegistrationInput i =
        new RegistrationInput(
            "  Ana ", " Novak ", " ana@example.com ", " private ", " ", "", null, List.of());

    RegistrationValidator.Result result = validator.validate(i);

    assertThat(result.valid()).as(String.valueOf(result.invalidFields())).isTrue();
    assertThat(result.participant().firstName()).isEqualTo("Ana");
    assertThat(result.participant().email()).isEqualTo("ana@example.com");
  }

  @ParameterizedTest
  @ValueSource(strings = {"O'Brien", "Anne-Marie", "J. R.", "José", "Ærøskøbing", "D’Angelo"})
  void acceptsCommonNameCharacters(String name) {
    assertThat(validator.validate(withFirstName(name)).valid()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "Ana\nBcc", "Ana\u0000", "<script>", "Ana1", "-Ana"})
  void rejectsInvalidNames(String name) {
    assertThat(validator.validate(withFirstName(name)).invalidFields())
        .containsExactly("firstName");
  }

  @Test
  void nameLengthLimitIsHundredCharacters() {
    assertThat(validator.validate(withFirstName("A".repeat(100))).valid()).isTrue();
    assertThat(validator.validate(withFirstName("A".repeat(101))).invalidFields())
        .containsExactly("firstName");
  }

  @Test
  void missingNameIsInvalid() {
    assertThat(validator.validate(withFirstName(null)).invalidFields())
        .containsExactly("firstName");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plain",
        "a@b",
        "a@@b.com",
        "a b@c.com",
        "a@b.com\r\nBcc: x@y.com",
        "a@b.com\nX: y",
        "a@-b.com",
        "a@b..com",
        "Ana <a@b.com>"
      })
  void rejectsInvalidEmails(String email) {
    assertThat(validator.validate(withEmail(email)).invalidFields()).containsExactly("email");
  }

  @Test
  void emailLengthLimitIs254Characters() {
    String domain = "@" + "d".repeat(60) + ".example.com";
    String ok = "a".repeat(254 - domain.length()) + domain;
    assertThat(ok).hasSize(254);
    assertThat(validator.validate(withEmail(ok)).valid()).isTrue();
    assertThat(validator.validate(withEmail("a" + ok)).invalidFields()).containsExactly("email");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Private", "government", ""})
  void rejectsUnknownPayerType(String payerType) {
    RegistrationInput i = privateInput();
    RegistrationInput input =
        new RegistrationInput(
            i.firstName(), i.lastName(), i.email(), payerType, null, null, null, null);
    assertThat(validator.validate(input).invalidFields()).containsExactly("payerType");
  }

  @Test
  void privatePayerMustNotSendCompanyData() {
    RegistrationInput i = privateInput();
    RegistrationInput input =
        new RegistrationInput(
            i.firstName(), i.lastName(), i.email(), "private", "X", "Y", "SI1", null);
    assertThat(validator.validate(input).invalidFields())
        .containsExactly("companyName", "companyAddress", "companyVatId");
  }

  @Test
  void companyPayerNeedsAllCompanyFields() {
    RegistrationInput i = companyInput();
    RegistrationInput input =
        new RegistrationInput(
            i.firstName(), i.lastName(), i.email(), "company", " ", null, "", null);
    assertThat(validator.validate(input).invalidFields())
        .containsExactly("companyName", "companyAddress", "companyVatId");
  }

  @ParameterizedTest
  @ValueSource(strings = {"S", "SI 123", "SI-123", "SI123456789012345678901", "ŠI123"})
  void rejectsInvalidVatIds(String vatId) {
    RegistrationInput i = companyInput();
    RegistrationInput input =
        new RegistrationInput(
            i.firstName(),
            i.lastName(),
            i.email(),
            "company",
            i.companyName(),
            i.companyAddress(),
            vatId,
            null);
    assertThat(validator.validate(input).invalidFields()).containsExactly("companyVatId");
  }

  @Test
  void companyTextLimitsAndControlCharacters() {
    RegistrationInput i = companyInput();
    RegistrationInput tooLong =
        new RegistrationInput(
            i.firstName(),
            i.lastName(),
            i.email(),
            "company",
            "N".repeat(201),
            "A".repeat(501),
            "SI1",
            null);
    assertThat(validator.validate(tooLong).invalidFields())
        .containsExactly("companyName", "companyAddress");
    RegistrationInput control =
        new RegistrationInput(
            i.firstName(), i.lastName(), i.email(), "company", "N\u0007", "A", "SI1", null);
    assertThat(validator.validate(control).invalidFields()).containsExactly("companyName");
    RegistrationInput atLimit =
        new RegistrationInput(
            i.firstName(),
            i.lastName(),
            i.email(),
            "company",
            "N".repeat(200),
            "A".repeat(500),
            "SI1",
            null);
    assertThat(validator.validate(atLimit).valid()).isTrue();
  }

  @Test
  void workshopRules() {
    assertThat(validator.validate(withWorkshops(null)).participant().workshopId()).isNull();
    assertThat(validator.validate(withWorkshops(List.of())).participant().workshopId()).isNull();
    assertThat(validator.validate(withWorkshops(List.of(" W2 "))).participant().workshopId())
        .isEqualTo("W2");
    assertThat(validator.validate(withWorkshops(List.of("W1", "W2"))).invalidFields())
        .containsExactly("workshops");
    assertThat(validator.validate(withWorkshops(List.of("W9"))).invalidFields())
        .containsExactly("workshops");
    assertThat(validator.validate(withWorkshops(List.of(""))).invalidFields())
        .containsExactly("workshops");
    assertThat(validator.validate(withWorkshops(Arrays.asList((String) null))).invalidFields())
        .containsExactly("workshops");
  }

  @Test
  void reportsAllInvalidFieldsTogether() {
    RegistrationInput input =
        new RegistrationInput(null, null, "x", "other", null, null, null, List.of("W9"));
    assertThat(validator.validate(input).invalidFields())
        .containsExactly("firstName", "lastName", "email", "payerType", "workshops");
    assertThat(validator.validate(input).participant()).isNull();
  }
}
