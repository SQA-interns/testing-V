package si.confreg.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import si.confreg.registration.application.BusinessSettings;

class ApplicationConfigurationTest {

  private final ApplicationConfiguration configuration = new ApplicationConfiguration();

  private static AppProperties properties(
      String tz, String deadline, String vat, String workshops, String testClock, String password) {
    return new AppProperties(
        tz,
        deadline,
        new BigDecimal("240.00"),
        new BigDecimal("300.00"),
        vat == null ? null : new BigDecimal(vat),
        workshops,
        100,
        testClock,
        "from@example.com",
        new AppProperties.Organizer("organizer", password));
  }

  private static AppProperties valid() {
    return properties(
        "Europe/Ljubljana", "2026-07-31", "0.22", "W1=One", "disabled", "x".repeat(16));
  }

  @Test
  void buildsBusinessSettingsFromProperties() {
    BusinessSettings settings = configuration.businessSettings(valid());

    assertThat(settings.feeSchedule().earlyBirdDeadline()).isEqualTo(LocalDate.of(2026, 7, 31));
    assertThat(settings.feeSchedule().conferenceZone().getId()).isEqualTo("Europe/Ljubljana");
    assertThat(settings.vatRate()).isEqualByComparingTo("0.22");
    assertThat(settings.workshops().find("W1")).isPresent();
  }

  @Test
  void rejectsInvalidBusinessSettings() {
    assertThatThrownBy(
            () ->
                configuration.businessSettings(
                    properties("Mars/Base", "2026-07-31", "0.22", "W1=One", "disabled", "p")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                configuration.businessSettings(
                    properties("Europe/Ljubljana", "31.7.2026", "0.22", "W1=One", "disabled", "p")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                configuration.businessSettings(
                    properties("Europe/Ljubljana", "2026-07-31", "1.5", "W1=One", "disabled", "p")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                configuration.businessSettings(
                    properties("Europe/Ljubljana", "2026-07-31", "0.22", "W1", "disabled", "p")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                configuration.businessSettings(
                    properties("", "2026-07-31", "0.22", "W1=One", "disabled", "p")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testClockMustNotBeEnabledInProduction() {
    MockEnvironment production = new MockEnvironment();
    production.setActiveProfiles("prod");
    AppProperties enabled =
        properties("Europe/Ljubljana", "2026-07-31", "0.22", "W1=One", "enabled", "p");

    assertThatThrownBy(() -> configuration.testClockSettings(enabled, production))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SR-04");
    assertThat(configuration.testClockSettings(valid(), production).enabled()).isFalse();
    assertThat(configuration.testClockSettings(enabled, new MockEnvironment()).enabled()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"on", "true", ""})
  void testClockAcceptsOnlyEnabledOrDisabled(String value) {
    AppProperties props =
        properties("Europe/Ljubljana", "2026-07-31", "0.22", "W1=One", value, "p");
    assertThatThrownBy(() -> configuration.testClockSettings(props, new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void organizerPasswordNeedsSixteenCharacters() {
    assertThat(configuration.organizerCredentialsCheck(valid())).isNotNull();
    AppProperties shortPassword =
        properties("Europe/Ljubljana", "2026-07-31", "0.22", "W1=One", "disabled", "x".repeat(15));
    assertThatThrownBy(() -> configuration.organizerCredentialsCheck(shortPassword))
        .isInstanceOf(IllegalStateException.class);
    AppProperties missing =
        properties("Europe/Ljubljana", "2026-07-31", "0.22", "W1=One", "disabled", null);
    assertThatThrownBy(() -> configuration.organizerCredentialsCheck(missing))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void organizerToStringHidesCredentials() {
    assertThat(new AppProperties.Organizer("alice-organizer", "secret-password").toString())
        .doesNotContain("alice-organizer")
        .doesNotContain("secret-password");
  }
}
