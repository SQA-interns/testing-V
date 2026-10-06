package si.confreg.registration.acceptance;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Fee by submission time and payer (US-001: AC-001-01, AC-001-02, AC-001-03, AC-001-06). */
class FeeAcceptanceTest extends AcceptanceTestBase {

  private void assertStoredWithFee(JsonNode created, BigDecimal net) {
    BigDecimal vat = vatOf(net, vatRate());
    assertAmounts(created, net, vat, net.add(vat));
    JsonNode stored = getAsOrganizer(created.get("registrationNumber").asString()).json();
    assertAmounts(stored, net, vat, net.add(vat));
  }

  @Test
  void ac_001_01_submissionOnOrBeforeDeadlineIsStoredWithEarlyBirdFee() {
    JsonNode created = register(privatePayer(uniqueEmail()), earlyInstant());
    assertStoredWithFee(created, feeEarly());
  }

  @Test
  void ac_001_01_submissionOnDeadlineDayIsStoredWithEarlyBirdFee() {
    JsonNode created = register(privatePayer(uniqueEmail()), localNoon(earlyBirdDeadline()));
    assertStoredWithFee(created, feeEarly());
  }

  @Test
  void ac_001_02_submissionAfterDeadlineIsStoredWithRegularFee() {
    JsonNode created = register(privatePayer(uniqueEmail()), regularInstant());
    assertStoredWithFee(created, feeRegular());
  }

  @Test
  void ac_001_03_lastSecondOfDeadlineDayInConferenceTimeZoneIsEarlyBird() {
    Instant lastEarlySecond = firstRegularInstant().minusSeconds(1);
    JsonNode created = register(privatePayer(uniqueEmail()), lastEarlySecond);
    assertStoredWithFee(created, feeEarly());
  }

  @Test
  void ac_001_03_firstSecondAfterDeadlineDayInConferenceTimeZoneIsRegular() {
    JsonNode created = register(privatePayer(uniqueEmail()), firstRegularInstant());
    assertStoredWithFee(created, feeRegular());
  }

  @Test
  void ac_001_06_companyFromOtherEuMemberStatePaysSlovenianVatEarlyBird() {
    JsonNode created = register(austrianCompany(uniqueEmail()), earlyInstant());
    assertStoredWithFee(created, feeEarly());
  }

  @Test
  void ac_001_06_companyFromOtherEuMemberStatePaysSlovenianVatRegular() {
    JsonNode created = register(austrianCompany(uniqueEmail()), regularInstant());
    assertStoredWithFee(created, feeRegular());
  }
}
