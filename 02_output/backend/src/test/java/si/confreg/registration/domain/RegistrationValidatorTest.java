package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator = new RegistrationValidator(Set.of("K1", "K2"));

  private static RegistrationInput privateInput(String email, String... workshops) {
    return new RegistrationInput(
        "Ana", "Kovač", email, "private", null, null, null, Arrays.asList(workshops));
  }

  private static RegistrationInput companyInput(String name, String address, String vatId) {
    return new RegistrationInput(
        "Žiga", "Šuštar", "z@example.com", "company", name, address, vatId, List.of());
  }

  private static List<String> fields(List<FieldError> errors) {
    return errors.stream().map(FieldError::field).toList();
  }

  @Test
  void validPrivateAndCompanyRegistrationsHaveNoErrors() {
    assertThat(validator.validate(privateInput("ana@example.com"))).isEmpty();
    assertThat(validator.validate(privateInput("ana@example.com", "K2"))).isEmpty();
    assertThat(validator.validate(companyInput("Primer d.o.o.", "Ulica 1\nLjubljana", "SI1")))
        .isEmpty();
  }

  @Test
  void collectsEveryMissingRequiredField() {
    RegistrationInput empty = new RegistrationInput(null, " ", "", null, null, null, null, null);

    assertThat(fields(validator.validate(empty)))
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "payerType");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"a@b", "a b@c.si", "a@b c.si", "@c.si", "a@", "a@@c.si", "a@.si", "a@c..si"})
  void rejectsMalformedEmail(String email) {
    assertThat(fields(validator.validate(privateInput(email)))).containsExactly("email");
  }

  @ParameterizedTest
  @ValueSource(strings = {"a@c.si", "first.last+tag@sub.example.com", "ž@primer.si"})
  void acceptsWellFormedEmail(String email) {
    assertThat(validator.validate(privateInput(email))).isEmpty();
  }

  @Test
  void emailAtLimitAcceptedAboveLimitRejectedOnceForLength() {
    String domain = "@example.com";
    String atLimit = "a".repeat(RegistrationValidator.EMAIL_MAX - domain.length()) + domain;

    assertThat(validator.validate(privateInput(atLimit))).isEmpty();
    assertThat(validator.validate(privateInput("a" + atLimit)))
        .containsExactly(new FieldError("email", "must not be longer than 254 characters"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Private", "PRIVATE", "person", "companies"})
  void rejectsUnknownPayerType(String payerType) {
    RegistrationInput input =
        new RegistrationInput("A", "B", "a@c.si", payerType, null, null, null, null);

    assertThat(validator.validate(input))
        .containsExactly(new FieldError("payerType", "must be 'private' or 'company'"));
  }

  @Test
  void companyRequiresAllCompanyFields() {
    assertThat(fields(validator.validate(companyInput(null, null, null))))
        .containsExactly("companyName", "companyAddress", "companyVatId");
  }

  @Test
  void privateRejectsEachCompanyField() {
    RegistrationInput input =
        new RegistrationInput("A", "B", "a@c.si", "private", "X", "Y", "Z", null);

    assertThat(validator.validate(input))
        .extracting(FieldError::message)
        .containsOnly("must be empty for a private payer")
        .hasSize(3);
  }

  @Test
  void lengthLimitsPerField() {
    RegistrationInput atLimits =
        new RegistrationInput(
            "x".repeat(RegistrationValidator.NAME_MAX),
            "y".repeat(RegistrationValidator.NAME_MAX),
            "a@c.si",
            "company",
            "n".repeat(RegistrationValidator.COMPANY_NAME_MAX),
            "d".repeat(RegistrationValidator.COMPANY_ADDRESS_MAX),
            "v".repeat(RegistrationValidator.VAT_ID_MAX),
            null);
    RegistrationInput overLimits =
        new RegistrationInput(
            atLimits.firstName() + "x",
            atLimits.lastName() + "y",
            "a@c.si",
            "company",
            atLimits.companyName() + "n",
            atLimits.companyAddress() + "d",
            atLimits.companyVatId() + "v",
            null);

    assertThat(validator.validate(atLimits)).isEmpty();
    assertThat(fields(validator.validate(overLimits)))
        .containsExactly("firstName", "lastName", "companyName", "companyAddress", "companyVatId");
  }

  @Test
  void controlCharactersRejectedExceptLineBreaksInAddress() {
    RegistrationInput withControls =
        new RegistrationInput(
            "A\u0007",
            "B\r\nBcc: x@y.si",
            "a@c.si",
            "company",
            "N\u0000",
            "Line 1\r\nLine 2",
            "V\t1",
            null);

    assertThat(fields(validator.validate(withControls)))
        .containsExactly("firstName", "lastName", "companyName", "companyVatId");
  }

  @Test
  void emailWithLineBreakRejected() {
    assertThat(fields(validator.validate(privateInput("a@c.si\r\nBcc: x@y.si")))).contains("email");
  }

  @Test
  void workshopRules() {
    assertThat(validator.validate(privateInput("a@c.si", "K1"))).isEmpty();
    assertThat(validator.validate(privateInput("a@c.si", "K3")))
        .containsExactly(new FieldError("workshops", "must be a configured workshop id"));
    assertThat(validator.validate(privateInput("a@c.si", "K1", "K2")))
        .containsExactly(new FieldError("workshops", "must contain at most one workshop"));
    assertThat(validator.validate(privateInput("a@c.si", "k1"))).hasSize(1);
    assertThat(validator.validate(privateInput("a@c.si", (String) null))).hasSize(1);
  }
}
