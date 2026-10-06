package si.confreg.registration.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.application.TestSettings;

class StartupGuardTest {

  private static AppProperties settings(String testClock, String smtpTls) {
    Map<String, String> values = TestSettings.defaults();
    values.put("testClock", testClock);
    values.put("smtpTls", smtpTls);
    return TestSettings.properties(values);
  }

  private static MockEnvironment profile(String... profiles) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profiles);
    return environment;
  }

  @Test
  void sr04_productionRefusesTestClock() {
    assertThatThrownBy(() -> new StartupGuard(profile("prod"), settings("enabled", "true")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_TEST_CLOCK");
  }

  @Test
  void sb04_productionRequiresSmtpTls() {
    assertThatThrownBy(() -> new StartupGuard(profile("prod"), settings("disabled", "false")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_SMTP_TLS");
  }

  @Test
  void productionWithSafeSettingsStarts() {
    assertThatCode(() -> new StartupGuard(profile("prod"), settings("disabled", "true")))
        .doesNotThrowAnyException();
  }

  @Test
  void otherProfilesMayUseTestClockWithoutTls() {
    assertThatCode(() -> new StartupGuard(profile(), settings("enabled", "false")))
        .doesNotThrowAnyException();
    assertThatCode(() -> new StartupGuard(profile("local"), settings("enabled", "false")))
        .doesNotThrowAnyException();
  }
}
