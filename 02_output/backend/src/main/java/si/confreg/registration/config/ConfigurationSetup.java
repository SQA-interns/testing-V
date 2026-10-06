package si.confreg.registration.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Enables {@link AppProperties} and refuses unsafe settings in the {@code prod} profile: test clock
 * (SR-04), plain SMTP (SB-04) and credentials over plain HTTP (SR-03, D-26).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
public class ConfigurationSetup implements InitializingBean {

  private final AppProperties properties;
  private final Environment environment;

  public ConfigurationSetup(AppProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  @Override
  public void afterPropertiesSet() {
    checkProduction(properties, environment.acceptsProfiles(Profiles.of("prod")));
  }

  static void checkProduction(AppProperties properties, boolean production) {
    if (!production) {
      return;
    }
    if (properties.testClockEnabled()) {
      throw new IllegalStateException("The test clock must not be enabled in production");
    }
    if (!properties.mailTls()) {
      throw new IllegalStateException("APP_MAIL_TLS must be true in production");
    }
    if (properties.insecureAuthAllowed()) {
      throw new IllegalStateException("APP_INSECURE_AUTH_ALLOWED must be false in production");
    }
  }
}
