package si.confreg.registration.config;

import org.springframework.core.env.Environment;

/**
 * Refuses to start with unsafe settings: the test clock outside the local and test profiles (SR-04,
 * spec 6), and a missing or short organizer password (ES-01, spec 3).
 */
final class StartupGuards {

  static final int MIN_PASSWORD_LENGTH = 16;

  private final AppProperties properties;
  private final Environment environment;

  StartupGuards(AppProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  void check() {
    if (properties.testClockEnabled()
        && !environment.acceptsProfiles(ApplicationConfig.LOCAL_OR_TEST)) {
      throw new IllegalStateException(
          "APP_TEST_CLOCK=enabled is only allowed with profile 'local' or 'test'; refusing to start");
    }
    String username = environment.getProperty("app.organizer.username");
    if (username == null || username.isBlank()) {
      throw new IllegalStateException("ORGANIZER_USERNAME is not set; refusing to start");
    }
    String password = environment.getProperty("app.organizer.password");
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "ORGANIZER_PASSWORD is missing or shorter than "
              + MIN_PASSWORD_LENGTH
              + " characters; refusing to start");
    }
  }
}
