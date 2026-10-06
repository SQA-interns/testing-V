package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AC-001-01, AC-001-02, AC-001-03, AC-001-06: fee by submission time, VAT for every payer. */
class FeeAcceptanceTest extends AcceptanceTestBase {

  private void assertStoredWithFee(
      Instant submittedAt, Map<String, Object> body, BigDecimal expectedNet) {
    ApiResponse created = registerSuccessfully(body, submittedAt);
    assertFee(created, expectedNet);
    assertFee(storedRegistration(registrationNumberOf(created)), expectedNet);
  }

  @Test
  @DisplayName("AC-001-01 early-bird fee well before the deadline")
  void ac001_01_earlyBirdFeeBeforeDeadline() {
    assertStoredWithFee(daysFromDeadline(-11), anaPrivate(), earlyFee());
  }

  @Test
  @DisplayName("AC-001-01 early-bird fee on the deadline day (inclusive)")
  void ac001_01_earlyBirdFeeOnDeadlineDay() {
    assertStoredWithFee(daysFromDeadline(0), anaPrivate(), earlyFee());
  }

  @Test
  @DisplayName("AC-001-02 regular fee after the deadline")
  void ac001_02_regularFeeAfterDeadline() {
    assertStoredWithFee(daysFromDeadline(5), anaPrivate(), regularFee());
  }

  @Test
  @DisplayName("AC-001-03 last second of the deadline day in the conference zone is early bird")
  void ac001_03_lastLocalSecondOfDeadlineIsEarlyBird() {
    assertStoredWithFee(lastEarlyBirdSecond(), anaPrivate(), earlyFee());
  }

  @Test
  @DisplayName("AC-001-03 first second after the deadline day in the conference zone is regular")
  void ac001_03_firstLocalSecondAfterDeadlineIsRegular() {
    assertStoredWithFee(firstRegularInstant(), anaPrivate(), regularFee());
  }

  @Test
  @DisplayName("AC-001-06 company from another EU member state pays Slovenian VAT (early bird)")
  void ac001_06_foreignCompanyEarlyBirdVat() {
    assertStoredWithFee(daysFromDeadline(-11), anaForAustrianCompany(), earlyFee());
  }

  @Test
  @DisplayName("AC-001-06 company from another EU member state pays Slovenian VAT (regular)")
  void ac001_06_foreignCompanyRegularVat() {
    assertStoredWithFee(daysFromDeadline(5), anaForAustrianCompany(), regularFee());
  }

  @Test
  @DisplayName("AC-001-06 domestic and foreign company payers get the same amounts")
  void ac001_06_sameAmountsForDomesticAndForeignCompany() {
    Instant now = daysFromDeadline(-2);
    ApiResponse domestic = registerSuccessfully(anaForSlovenianCompany(), now);
    ApiResponse foreign = registerSuccessfully(anaForAustrianCompany(), now);
    for (String field : List.of("netFee", "vat", "grossFee")) {
      assertThat(amount(foreign.json(), field))
          .as(field)
          .isEqualByComparingTo(amount(domestic.json(), field));
    }
  }
}
