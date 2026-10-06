package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppPropertiesTest {

  @Test
  void parsesAllSettings() {
    AppProperties properties = TestSettings.properties();

    assertThat(properties.conferenceTz()).isEqualTo(ZoneId.of("Europe/Ljubljana"));
    assertThat(properties.earlyBirdDeadline()).isEqualTo(LocalDate.of(2026, 7, 31));
    assertThat(properties.feeEarly()).isEqualByComparingTo("240.00");
    assertThat(properties.feeRegular()).isEqualByComparingTo("300.00");
    assertThat(properties.vatRate()).isEqualByComparingTo("0.22");
    assertThat(properties.rateLimitPerHour()).isEqualTo(3);
    assertThat(properties.testClockEnabled()).isFalse();
    assertThat(properties.mailFrom()).isEqualTo("registration@test.example");
    assertThat(properties.smtpHost()).isEqualTo("localhost");
    assertThat(properties.smtpPort()).isEqualTo(1025);
    assertThat(properties.smtpTls()).isFalse();
  }

  @Test
  void workshopsKeepOrderAndTrimAndAreReadOnly() {
    AppProperties properties = TestSettings.with("workshops", " W2 = Second ;;W1=First;");

    assertThat(properties.workshops())
        .containsExactly(Map.entry("W2", "Second"), Map.entry("W1", "First"));
    assertThat(properties.workshopIds()).containsExactly("W2", "W1");
    assertThatThrownBy(() -> properties.workshops().put("W9", "x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void emptyWorkshopListIsAllowed() {
    assertThat(TestSettings.with("workshops", "").workshops()).isEmpty();
    assertThat(TestSettings.with("workshops", null).workshops()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"W1", "=Title", "W1=", "W1=A;W1=B"})
  void invalidWorkshopListStopsStartup(String raw) {
    assertThatThrownBy(() -> TestSettings.with("workshops", raw))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid value for setting APP_WORKSHOPS");
  }

  @ParameterizedTest
  @CsvSource({
    "conferenceTz, Mars/Olympus, APP_CONFERENCE_TZ",
    "earlyBirdDeadline, 31.07.2026, APP_EARLY_BIRD_DEADLINE",
    "feeEarly, abc, APP_FEE_EARLY",
    "feeRegular, -1, APP_FEE_REGULAR",
    "vatRate, x, APP_VAT_RATE",
    "rateLimitPerHour, 0, APP_RATE_LIMIT_PER_HOUR",
    "rateLimitPerHour, many, APP_RATE_LIMIT_PER_HOUR",
    "testClock, on, APP_TEST_CLOCK",
    "smtpPort, -5, APP_SMTP_PORT",
    "smtpTls, yes, APP_SMTP_TLS"
  })
  void invalidValueNamesTheSetting(String name, String value, String setting) {
    assertThatThrownBy(() -> TestSettings.with(name, value))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid value for setting " + setting);
  }

  @ParameterizedTest
  @CsvSource({
    "conferenceTz, APP_CONFERENCE_TZ",
    "earlyBirdDeadline, APP_EARLY_BIRD_DEADLINE",
    "feeEarly, APP_FEE_EARLY",
    "mailFrom, APP_MAIL_FROM",
    "smtpHost, APP_SMTP_HOST"
  })
  void missingRequiredValueNamesTheSetting(String name, String setting) {
    assertThatThrownBy(() -> TestSettings.with(name, " "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required setting " + setting);
  }

  @Test
  void testClockFlagValues() {
    assertThat(TestSettings.with("testClock", "enabled").testClockEnabled()).isTrue();
    assertThat(TestSettings.with("testClock", "").testClockEnabled()).isFalse();
    assertThat(TestSettings.with("testClock", null).testClockEnabled()).isFalse();
  }

  @Test
  void zeroFeeIsAllowed() {
    assertThat(TestSettings.with("feeEarly", "0").feeEarly()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void smtpTlsTrue() {
    assertThat(TestSettings.with("smtpTls", "true").smtpTls()).isTrue();
  }
}
