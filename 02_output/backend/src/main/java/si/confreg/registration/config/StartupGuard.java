package si.confreg.registration.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.AppProperties;

/**
 * Refuses to start production (profile {@code prod}) with the test clock enabled (SR-04) or without
 * TLS to the SMTP server (SB-04).
 */
@Component
public final class StartupGuard {

  public StartupGuard(Environment environment, AppProperties properties) {
    check(environment.acceptsProfiles(Profiles.of("prod")), properties);
  }

  static void check(boolean production, AppProperties properties) {
    if (!production) {
      return;
    }
    if (properties.testClockEnabled()) {
      throw new IllegalStateException("APP_TEST_CLOCK must not be enabled in production");
    }
    if (!properties.smtpTls()) {
      throw new IllegalStateException("APP_SMTP_TLS must be true in production");
    }
  }
}
