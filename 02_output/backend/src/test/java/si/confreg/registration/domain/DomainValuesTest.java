package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** Unit tests of the small domain value types. */
class DomainValuesTest {

  @Test
  void payerTypeLookupIsExactAndCaseSensitive() {
    assertThat(PayerType.fromCode("private")).contains(PayerType.PRIVATE);
    assertThat(PayerType.fromCode("company")).contains(PayerType.COMPANY);
    assertThat(PayerType.fromCode("Company")).isEmpty();
    assertThat(PayerType.fromCode(" private")).isEmpty();
    assertThat(PayerType.fromCode(null)).isEmpty();
    assertThat(PayerType.COMPANY.code()).isEqualTo("company");
  }

  @Test
  void privatePayerHasNoCompanyData() {
    assertThat(Payer.privatePerson().companyName()).isNull();
    assertThatThrownBy(() -> new Payer(PayerType.PRIVATE, "X", null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Payer(PayerType.PRIVATE, null, "X", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Payer(PayerType.PRIVATE, null, null, "X"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void companyPayerNeedsAllCompanyData() {
    assertThat(Payer.company("N", "A", "V").type()).isEqualTo(PayerType.COMPANY);
    assertThatThrownBy(() -> Payer.company(null, "A", "V"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Payer.company("N", null, "V"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Payer.company("N", "A", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Payer(null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void feeGrossMustEqualNetPlusVat() {
    assertThat(new Fee(new BigDecimal("1.00"), new BigDecimal("0.10"), new BigDecimal("1.10")))
        .isNotNull();
    assertThatThrownBy(
            () -> new Fee(new BigDecimal("1.00"), new BigDecimal("0.10"), new BigDecimal("1.11")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Fee(null, BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void workshopCatalogParsesConfiguredEncodingInOrder() {
    WorkshopCatalog catalog = WorkshopCatalog.parse(" B = Beta title ; A=Alpha;; C=Gamma = more ");

    assertThat(catalog.all())
        .containsExactly(
            new Workshop("B", "Beta title"),
            new Workshop("A", "Alpha"),
            new Workshop("C", "Gamma = more"));
    assertThat(catalog.contains("A")).isTrue();
    assertThat(catalog.contains("a")).isFalse();
    assertThat(catalog.find("C")).map(Workshop::title).contains("Gamma = more");
    assertThat(catalog.find("Z")).isEmpty();
  }

  @Test
  void workshopCatalogRejectsMalformedEntries() {
    assertThat(WorkshopCatalog.parse(null).all()).isEmpty();
    assertThat(WorkshopCatalog.parse(" ; ").all()).isEmpty();
    assertThatThrownBy(() -> WorkshopCatalog.parse("A"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> WorkshopCatalog.parse("=Title"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> WorkshopCatalog.parse("A= "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> WorkshopCatalog.parse("A=x;A=y"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registrationNumbersAreWellFormedAndUseTheRandomSource() {
    RegistrationNumbers numbers = new RegistrationNumbers();
    for (int i = 0; i < 200; i++) {
      assertThat(RegistrationNumbers.isWellFormed(numbers.next())).isTrue();
    }
    RandomGenerator zeros = () -> 0L;
    assertThat(new RegistrationNumbers(zeros).next()).isEqualTo("REG-0000000000");
  }

  @Test
  void registrationNumberFormatCheck() {
    assertThat(RegistrationNumbers.isWellFormed("REG-0123456789")).isTrue();
    assertThat(RegistrationNumbers.isWellFormed("REG-ABCDEFGHJK")).isTrue();
    assertThat(RegistrationNumbers.isWellFormed("REG-ABCDEFGHIK")).as("I excluded").isFalse();
    assertThat(RegistrationNumbers.isWellFormed("REG-012345678")).isFalse();
    assertThat(RegistrationNumbers.isWellFormed("reg-0123456789")).isFalse();
    assertThat(RegistrationNumbers.isWellFormed("REG-0123456789 ")).isFalse();
    assertThat(RegistrationNumbers.isWellFormed(null)).isFalse();
  }

  @Test
  void registrationKeepsItsValuesAndChangesOnlyTheNumber() {
    Fee fee = new Fee(new BigDecimal("1.00"), new BigDecimal("0.10"), new BigDecimal("1.10"));
    Instant at = Instant.parse("2030-01-01T00:00:00Z");
    Registration registration =
        new Registration("REG-0000000001", "F", "L", "e@x.y", Payer.privatePerson(), "W", fee, at);

    Registration renumbered = registration.withRegistrationNumber("REG-0000000002");

    assertThat(renumbered.registrationNumber()).isEqualTo("REG-0000000002");
    assertThat(renumbered)
        .usingRecursiveComparison()
        .ignoringFields("registrationNumber")
        .isEqualTo(registration);
    assertThat(registration.status()).isEqualTo(Registration.STATUS_REGISTERED);
    assertThatThrownBy(
            () -> new Registration(null, "F", "L", "e", Payer.privatePerson(), null, fee, at))
        .isInstanceOf(NullPointerException.class);
  }
}
