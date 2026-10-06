package si.confreg.registration.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of project/00_setup/environments.md, bound from application.properties, which reads the
 * environment variables of the same name (AR-04, ES-01). Defaults live only in
 * application.properties; secrets have none.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String conferenceTz,
    String earlyBirdDeadline,
    BigDecimal feeEarly,
    BigDecimal feeRegular,
    BigDecimal vatRate,
    String workshops,
    int rateLimitPerHour,
    String testClock,
    String mailFrom,
    Organizer organizer) {

  /** Organizer credentials (ORGANIZER_USERNAME, ORGANIZER_PASSWORD). */
  public record Organizer(String username, String password) {
    @Override
    public String toString() {
      return "Organizer[username=***, password=***]";
    }
  }
}
