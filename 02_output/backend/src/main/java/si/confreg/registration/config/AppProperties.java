package si.confreg.registration.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Business and runtime settings {@code app.*}, bound from the {@code APP_*} environment variables
 * (spec 3, AR-04). Defaults live only in {@code application.yml}. The organizer password is
 * deliberately not bound here (spec 7.1).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    ZoneId conferenceTz,
    LocalDate earlyBirdDeadline,
    BigDecimal feeEarly,
    BigDecimal feeRegular,
    BigDecimal vatRate,
    String workshops,
    int rateLimitPerHour,
    String testClock,
    String mailFrom) {

  /** {@code APP_TEST_CLOCK=enabled}; any other value means disabled. */
  public boolean testClockEnabled() {
    return "enabled".equalsIgnoreCase(testClock == null ? "" : testClock.strip());
  }
}
