package si.confreg.registration.config;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import si.confreg.registration.application.BusinessSettings;
import si.confreg.registration.application.TimeSource;
import si.confreg.registration.clock.ApplicationTimeSource;
import si.confreg.registration.clock.TestClockFilter;
import si.confreg.registration.clock.TestClockSettings;
import si.confreg.registration.domain.FeeSchedule;
import si.confreg.registration.domain.WorkshopCatalogue;

/** Wiring and startup checks (docs/02_specification.md, section 3). */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfiguration {

  static final int MIN_ORGANIZER_PASSWORD_LENGTH = 16;

  @Bean
  BusinessSettings businessSettings(AppProperties properties) {
    ZoneId zone;
    LocalDate deadline;
    try {
      zone = ZoneId.of(required(properties.conferenceTz(), "APP_CONFERENCE_TZ"));
      deadline =
          LocalDate.parse(required(properties.earlyBirdDeadline(), "APP_EARLY_BIRD_DEADLINE"));
    } catch (DateTimeException e) {
      throw new IllegalStateException("APP_CONFERENCE_TZ or APP_EARLY_BIRD_DEADLINE is invalid", e);
    }
    BigDecimal early = amount(properties.feeEarly(), "APP_FEE_EARLY");
    BigDecimal regular = amount(properties.feeRegular(), "APP_FEE_REGULAR");
    BigDecimal vatRate = properties.vatRate();
    if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalStateException("APP_VAT_RATE must be between 0 and 1");
    }
    WorkshopCatalogue workshops;
    try {
      workshops = WorkshopCatalogue.parse(required(properties.workshops(), "APP_WORKSHOPS"));
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("APP_WORKSHOPS is invalid", e);
    }
    return new BusinessSettings(
        new FeeSchedule(deadline, zone, early, regular), vatRate, workshops);
  }

  @Bean
  TestClockSettings testClockSettings(AppProperties properties, Environment environment) {
    String value = required(properties.testClock(), "APP_TEST_CLOCK");
    if (!"enabled".equals(value) && !"disabled".equals(value)) {
      throw new IllegalStateException("APP_TEST_CLOCK must be enabled or disabled");
    }
    boolean enabled = "enabled".equals(value);
    if (enabled && environment.matchesProfiles("prod")) {
      throw new IllegalStateException("The test clock must not be enabled in production (SR-04)");
    }
    return new TestClockSettings(enabled);
  }

  @Bean
  TimeSource timeSource(TestClockSettings settings) {
    return new ApplicationTimeSource(settings);
  }

  @Bean
  FilterRegistrationBean<TestClockFilter> testClockFilter(TestClockSettings settings) {
    FilterRegistrationBean<TestClockFilter> registration =
        new FilterRegistrationBean<>(new TestClockFilter(settings));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  OrganizerCredentialsCheck organizerCredentialsCheck(AppProperties properties) {
    AppProperties.Organizer organizer = properties.organizer();
    if (organizer == null || isBlank(organizer.username()) || organizer.password() == null) {
      throw new IllegalStateException("ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set");
    }
    if (organizer.password().length() < MIN_ORGANIZER_PASSWORD_LENGTH) {
      throw new IllegalStateException("ORGANIZER_PASSWORD must have at least 16 characters");
    }
    return new OrganizerCredentialsCheck();
  }

  /** Marker bean: the organizer credentials passed the startup check. */
  static final class OrganizerCredentialsCheck {}

  private static String required(String value, String name) {
    if (isBlank(value)) {
      throw new IllegalStateException(name + " must be set");
    }
    return value.strip();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static BigDecimal amount(BigDecimal value, String name) {
    if (value == null || value.signum() < 0) {
      throw new IllegalStateException(name + " must be a non-negative amount");
    }
    return value;
  }
}
