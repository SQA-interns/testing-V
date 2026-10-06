package si.confreg.registration.config;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start production with the test clock enabled (SR-04) or without SMTP TLS (SB-04). Runs
 * before the application reports ready.
 */
@Component
public class StartupGuard {

  static final String PRODUCTION = "prod";
  static final String SMTP_TLS = "spring.mail.properties.mail.smtp.starttls.required";

  private final AppSettings settings;
  private final Environment environment;

  public StartupGuard(AppSettings settings, Environment environment) {
    this.settings = settings;
    this.environment = environment;
  }

  @EventListener(ApplicationStartedEvent.class)
  public void check() {
    settings.workshopTitles();
    if (!environment.acceptsProfiles(Profiles.of(PRODUCTION))) {
      return;
    }
    if (settings.testClockEnabled()) {
      throw new IllegalStateException("The test clock must not be enabled in production");
    }
    if (!environment.getProperty(SMTP_TLS, Boolean.class, false)) {
      throw new IllegalStateException("SMTP_TLS must be true in production");
    }
  }
}
