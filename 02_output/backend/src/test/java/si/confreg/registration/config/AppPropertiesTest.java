package si.confreg.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

class AppPropertiesTest {

  static AppProperties properties(
      String zone,
      String deadline,
      String early,
      String regular,
      String vat,
      String workshops,
      int rateLimit,
      String testClock,
      boolean mailTls,
      boolean insecureAuth) {
    return new AppProperties(
        zone,
        deadline,
        early == null ? null : new BigDecimal(early),
        regular == null ? null : new BigDecimal(regular),
        vat == null ? null : new BigDecimal(vat),
        workshops,
        rateLimit,
        testClock,
        "from@example.com",
        mailTls,
        insecureAuth);
  }

  static AppProperties valid() {
    return properties(
        "Europe/Ljubljana",
        "2030-01-31",
        "10.00",
        "20.00",
        "0.1",
        "K1=Kappa;K2=Lambda",
        5,
        "disabled",
        false,
        false);
  }

  @Test
  void parsesValidSettings() {
    AppProperties properties = valid();

    assertThat(properties.conferenceZone()).isEqualTo(ZoneId.of("Europe/Ljubljana"));
    assertThat(properties.earlyBirdDeadlineDate()).isEqualTo(LocalDate.of(2030, 1, 31));
    assertThat(properties.workshopCatalogue())
        .containsExactly(
            org.assertj.core.api.Assertions.entry("K1", "Kappa"),
            org.assertj.core.api.Assertions.entry("K2", "Lambda"));
    assertThat(properties.testClockEnabled()).isFalse();
  }

  @Test
  void workshopEntriesAreTrimmedAndCatalogueIsUnmodifiable() {
    AppProperties properties =
        properties("UTC", "2030-01-31", "1", "2", "0", " K1 = A b ; K2=C=D", 1, "", false, false);

    assertThat(properties.workshopCatalogue())
        .containsEntry("K1", "A b")
        .containsEntry("K2", "C=D");
    assertThatThrownBy(() -> properties.workshopCatalogue().put("X", "Y"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void workshopIdOfTwentyCharactersAccepted() {
    String id = "x".repeat(20);

    assertThat(
            properties("UTC", "2030-01-31", "1", "2", "0", id + "=A", 1, "", false, false)
                .workshopCatalogue())
        .containsKey(id);
  }

  @ParameterizedTest
  @ValueSource(strings = {"enabled", "ENABLED", " Enabled "})
  void testClockEnabledValues(String value) {
    assertThat(
            properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, value, false, false)
                .testClockEnabled())
        .isTrue();
  }

  @Test
  void rejectsInvalidSettings() {
    assertInvalid("Mars/Base", "2030-01-31", "1", "2", "0", "K=A", 1, "disabled", "time zone");
    assertInvalid(null, "2030-01-31", "1", "2", "0", "K=A", 1, "disabled", "conference-tz");
    assertInvalid("UTC", "31.1.2030", "1", "2", "0", "K=A", 1, "disabled", "ISO date");
    assertInvalid("UTC", null, "1", "2", "0", "K=A", 1, "disabled", "early-bird-deadline");
    assertInvalid("UTC", "2030-01-31", "0", "2", "0", "K=A", 1, "disabled", "fee-early");
    assertInvalid("UTC", "2030-01-31", "1", null, "0", "K=A", 1, "disabled", "fee-regular");
    assertInvalid("UTC", "2030-01-31", "1", "2", "1", "K=A", 1, "disabled", "vat-rate");
    assertInvalid("UTC", "2030-01-31", "1", "2", "-0.1", "K=A", 1, "disabled", "vat-rate");
    assertInvalid("UTC", "2030-01-31", "1", "2", null, "K=A", 1, "disabled", "vat-rate");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", " ", 1, "disabled", "workshops");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "K", 1, "disabled", "id=name");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "=A", 1, "disabled", "id=name");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "K=", 1, "disabled", "invalid id");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "K=A;K=B", 1, "disabled", "duplicate");
    assertInvalid(
        "UTC", "2030-01-31", "1", "2", "0", "x".repeat(21) + "=A", 1, "disabled", "invalid id");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "K=A", 0, "disabled", "rate-limit");
    assertInvalid("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "on", "test-clock");
  }

  @Test
  void rejectsMissingSender() {
    assertThatThrownBy(
            () ->
                new AppProperties(
                    "UTC",
                    "2030-01-31",
                    BigDecimal.ONE,
                    BigDecimal.TWO,
                    BigDecimal.ZERO,
                    "K=A",
                    1,
                    "disabled",
                    " ",
                    false,
                    false))
        .hasMessageContaining("mail-from");
  }

  @Test
  void productionRefusesUnsafeSettings() {
    AppProperties testClock =
        properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "enabled", true, false);
    AppProperties plainSmtp =
        properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "disabled", false, false);
    AppProperties insecureAuth =
        properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "disabled", true, true);
    AppProperties safe =
        properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "disabled", true, false);

    assertThatThrownBy(() -> ConfigurationSetup.checkProduction(testClock, true))
        .hasMessageContaining("test clock");
    assertThatThrownBy(() -> ConfigurationSetup.checkProduction(plainSmtp, true))
        .hasMessageContaining("APP_MAIL_TLS");
    assertThatThrownBy(() -> ConfigurationSetup.checkProduction(insecureAuth, true))
        .hasMessageContaining("APP_INSECURE_AUTH_ALLOWED");
    ConfigurationSetup.checkProduction(safe, true);
    ConfigurationSetup.checkProduction(testClock, false);
    ConfigurationSetup.checkProduction(insecureAuth, false);
  }

  @Test
  void productionProfileCheckedOnStartup() {
    MockEnvironment production = new MockEnvironment();
    production.setActiveProfiles("prod");
    AppProperties testClock =
        properties("UTC", "2030-01-31", "1", "2", "0", "K=A", 1, "enabled", true, false);

    assertThatThrownBy(() -> new ConfigurationSetup(testClock, production).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class);
    new ConfigurationSetup(testClock, new MockEnvironment()).afterPropertiesSet();
  }

  @Test
  @SuppressWarnings("deprecation")
  void organizerAccountRequiresCredentials() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("organizer.username", "org")
            .withProperty("organizer.password", "p".repeat(OrganizerAccount.MIN_PASSWORD_LENGTH));

    UserDetails user =
        OrganizerAccount.fromEnvironment(environment, NoOpPasswordEncoder.getInstance());

    assertThat(user.getUsername()).isEqualTo("org");
    assertThat(user.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ORGANIZER");
    assertThatThrownBy(
            () ->
                OrganizerAccount.fromEnvironment(
                    new MockEnvironment().withProperty("organizer.password", "p".repeat(16)),
                    NoOpPasswordEncoder.getInstance()))
        .hasMessageContaining("ORGANIZER_USERNAME");
    assertThatThrownBy(
            () ->
                OrganizerAccount.fromEnvironment(
                    new MockEnvironment()
                        .withProperty("organizer.username", "org")
                        .withProperty("organizer.password", "p".repeat(15)),
                    NoOpPasswordEncoder.getInstance()))
        .hasMessageContaining("ORGANIZER_PASSWORD");
  }

  private static void assertInvalid(
      String zone,
      String deadline,
      String early,
      String regular,
      String vat,
      String workshops,
      int rateLimit,
      String testClock,
      String message) {
    assertThatThrownBy(
            () ->
                properties(
                    zone, deadline, early, regular, vat, workshops, rateLimit, testClock, false,
                    false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(message);
  }
}
