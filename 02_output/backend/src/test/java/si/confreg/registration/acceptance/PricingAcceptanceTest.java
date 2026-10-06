package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.FEE_EARLY;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.FEE_REGULAR;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.grossOf;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.vatOf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Registrations;

/** US-001 [2], [3]: the price of a registration, from the configured business values. */
class PricingAcceptanceTest extends AcceptanceTestBase {

  private void assertPrice(ApiClient.Response response, BigDecimal net) {
    assertThat(response.amount("netFee")).isEqualByComparingTo(net);
    assertThat(response.amount("vat")).isEqualByComparingTo(vatOf(net));
    assertThat(response.amount("grossFee")).isEqualByComparingTo(grossOf(net));
  }

  @Test
  void ac001_02_earlyBirdFeeBeforeDeadline() {
    ApiClient.Response response =
        registerOk(Registrations.privatePerson(), AcceptanceConfig.earlyBirdTime());

    assertPrice(response, FEE_EARLY);
  }

  @Test
  void ac001_02_regularFeeAfterDeadline() {
    ApiClient.Response response =
        registerOk(Registrations.privatePerson(), AcceptanceConfig.regularTime());

    assertPrice(response, FEE_REGULAR);
  }

  @Test
  void ac001_03_studentRegistersForFreeBeforeAndAfterDeadline() {
    for (Instant now : List.of(AcceptanceConfig.earlyBirdTime(), AcceptanceConfig.regularTime())) {
      Map<String, Object> body = Registrations.with(Registrations.privatePerson(), "student", true);

      ApiClient.Response response = registerOk(body, now);

      assertPrice(response, BigDecimal.ZERO);
      String number = (String) response.json().get("registrationNumber");
      assertThat(api().getAsOrganizer("/api/registrations/" + number).json())
          .containsEntry("student", true);
    }
  }

  @Test
  void ac001_06_lastSecondOfDeadlineDayInConferenceTimeZoneIsEarlyBird() {
    ApiClient.Response response =
        registerOk(Registrations.privatePerson(), AcceptanceConfig.lastEarlyBirdSecond());

    assertPrice(response, FEE_EARLY);
  }

  @Test
  void ac001_07_midnightAfterDeadlineInConferenceTimeZoneIsRegularThoughStillDeadlineDateInUtc() {
    Instant firstRegular = AcceptanceConfig.firstRegularInstant();
    assertThat(firstRegular.toString()).startsWith(AcceptanceConfig.EARLY_BIRD_DEADLINE.toString());

    ApiClient.Response response = registerOk(Registrations.privatePerson(), firstRegular);

    assertPrice(response, FEE_REGULAR);
  }

  @Test
  void ac001_08_vatIsRoundedHalfUpAndAmountsHaveTwoDecimals() {
    // The configured regular fee makes half-up and half-even rounding differ.
    BigDecimal exact = FEE_REGULAR.multiply(AcceptanceConfig.VAT_RATE);
    assertThat(exact.setScale(2, RoundingMode.HALF_UP))
        .isNotEqualByComparingTo(exact.setScale(2, RoundingMode.HALF_EVEN));

    ApiClient.Response response =
        registerOk(Registrations.privatePerson(), AcceptanceConfig.regularTime());

    assertThat(response.rawAmount("vat")).isEqualTo(vatOf(FEE_REGULAR).toPlainString());
    assertThat(response.rawAmount("netFee")).matches("[0-9]+\\.[0-9]{2}");
    assertThat(response.rawAmount("vat")).matches("[0-9]+\\.[0-9]{2}");
    assertThat(response.rawAmount("grossFee")).matches("[0-9]+\\.[0-9]{2}");

    String number = (String) response.json().get("registrationNumber");
    ApiClient.Response stored = api().getAsOrganizer("/api/registrations/" + number);
    assertThat(stored.rawAmount("grossFee")).isEqualTo(grossOf(FEE_REGULAR).toPlainString());
  }

  @Test
  void ac001_08_studentAmountsHaveTwoDecimals() {
    ApiClient.Response response =
        registerOk(
            Registrations.with(Registrations.privatePerson(), "student", true),
            AcceptanceConfig.regularTime());

    assertThat(response.rawAmount("netFee")).isEqualTo("0.00");
    assertThat(response.rawAmount("vat")).isEqualTo("0.00");
    assertThat(response.rawAmount("grossFee")).isEqualTo("0.00");
  }

  @Test
  void ac001_09_companyWithVatIdPaysSameFeeAndVatAsPrivatePayer() {
    ApiClient.Response early =
        registerOk(Registrations.company(), AcceptanceConfig.earlyBirdTime());
    ApiClient.Response regular =
        registerOk(Registrations.company(), AcceptanceConfig.regularTime());

    assertPrice(early, FEE_EARLY);
    assertPrice(regular, FEE_REGULAR);
  }

  @Test
  void ac001_10_workshopDoesNotChangeTheFee() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.privatePerson(), "workshops", List.of(AcceptanceConfig.WORKSHOP_A));

    assertPrice(registerOk(body, AcceptanceConfig.earlyBirdTime()), FEE_EARLY);
  }
}
