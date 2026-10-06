package si.confreg.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Unit tests of the start-up refusals (SR-04, ES-01). */
class StartupGuardsTest {

  private static AppProperties properties(String testClock) {
    return new AppProperties(
        ZoneOffset.UTC,
        LocalDate.of(2031, 1, 1),
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        "T=t",
        1,
        testClock,
        "a@test.example");
  }

  private static MockEnvironment environment(String... profiles) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profiles);
    environment.setProperty("app.organizer.username", "organizer");
    environment.setProperty(
        "app.organizer.password", "x".repeat(StartupGuards.MIN_PASSWORD_LENGTH));
    return environment;
  }

  @Test
  void testClockIsRefusedOutsideLocalAndTest() {
    assertThatThrownBy(() -> new StartupGuards(properties("enabled"), environment()).check())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_TEST_CLOCK");
    assertThatThrownBy(
            () -> new StartupGuards(properties(" ENABLED "), environment("production")).check())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testClockIsAllowedInLocalAndTest() {
    assertThatCode(() -> new StartupGuards(properties("enabled"), environment("local")).check())
        .doesNotThrowAnyException();
    assertThatCode(() -> new StartupGuards(properties("enabled"), environment("test")).check())
        .doesNotThrowAnyException();
  }

  @Test
  void disabledTestClockStartsEverywhere() {
    assertThatCode(() -> new StartupGuards(properties("disabled"), environment()).check())
        .doesNotThrowAnyException();
    assertThat(properties(null).testClockEnabled()).isFalse();
    assertThat(properties("yes").testClockEnabled()).isFalse();
  }

  @Test
  void organizerCredentialsAreRequired() {
    MockEnvironment shortPassword = environment();
    shortPassword.setProperty(
        "app.organizer.password", "x".repeat(StartupGuards.MIN_PASSWORD_LENGTH - 1));
    assertThatThrownBy(() -> new StartupGuards(properties("disabled"), shortPassword).check())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ORGANIZER_PASSWORD")
        .hasMessageNotContaining("xxxx");

    MockEnvironment noUser = environment();
    noUser.setProperty("app.organizer.username", " ");
    assertThatThrownBy(() -> new StartupGuards(properties("disabled"), noUser).check())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ORGANIZER_USERNAME");
  }
}
