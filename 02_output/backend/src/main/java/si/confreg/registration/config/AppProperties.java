package si.confreg.registration.config;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Business and runtime settings from {@code project/00_setup/environments.md}, bound from {@code
 * app.*} (environment variables {@code APP_*}). Invalid values stop the application at startup.
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
    boolean mailTls,
    boolean insecureAuthAllowed) {

  private static final int MAX_WORKSHOP_ID_LENGTH = 20;

  public AppProperties {
    zone(conferenceTz);
    deadline(earlyBirdDeadline);
    requirePositive("app.fee-early", feeEarly);
    requirePositive("app.fee-regular", feeRegular);
    if (vatRate == null
        || vatRate.compareTo(BigDecimal.ZERO) < 0
        || vatRate.compareTo(BigDecimal.ONE) >= 0) {
      throw new IllegalStateException("app.vat-rate must be at least 0 and below 1");
    }
    parseWorkshops(workshops);
    if (rateLimitPerHour < 1) {
      throw new IllegalStateException("app.rate-limit-per-hour must be at least 1");
    }
    testClockEnabled(testClock);
    if (mailFrom == null || mailFrom.isBlank()) {
      throw new IllegalStateException("app.mail-from must be set");
    }
  }

  /** Conference time zone; every business date is interpreted in it (AR-05). */
  public ZoneId conferenceZone() {
    return zone(conferenceTz);
  }

  /** Last day of the early-bird period (inclusive). */
  public LocalDate earlyBirdDeadlineDate() {
    return deadline(earlyBirdDeadline);
  }

  /** Configured workshops: id to name, in configuration order. */
  public Map<String, String> workshopCatalogue() {
    return parseWorkshops(workshops);
  }

  public boolean testClockEnabled() {
    return testClockEnabled(testClock);
  }

  private static ZoneId zone(String value) {
    if (value == null) {
      throw new IllegalStateException("app.conference-tz must be set");
    }
    try {
      return ZoneId.of(value);
    } catch (DateTimeException e) {
      throw new IllegalStateException("app.conference-tz is not a valid time zone", e);
    }
  }

  private static LocalDate deadline(String value) {
    if (value == null) {
      throw new IllegalStateException("app.early-bird-deadline must be set");
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeException e) {
      throw new IllegalStateException("app.early-bird-deadline is not an ISO date", e);
    }
  }

  private static void requirePositive(String name, BigDecimal value) {
    if (value == null || value.signum() <= 0) {
      throw new IllegalStateException(name + " must be a positive amount");
    }
  }

  private static boolean testClockEnabled(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "enabled" -> true;
      case "disabled", "" -> false;
      default -> throw new IllegalStateException("app.test-clock must be enabled or disabled");
    };
  }

  private static Map<String, String> parseWorkshops(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("app.workshops must list at least one workshop");
    }
    Map<String, String> catalogue = new LinkedHashMap<>();
    for (String entry : value.split(";")) {
      int separator = entry.indexOf('=');
      if (separator <= 0) {
        throw new IllegalStateException("app.workshops entries must be id=name");
      }
      String id = entry.substring(0, separator).trim();
      String name = entry.substring(separator + 1).trim();
      if (id.isEmpty() || id.length() > MAX_WORKSHOP_ID_LENGTH || name.isEmpty()) {
        throw new IllegalStateException("app.workshops has an invalid id or name");
      }
      if (catalogue.putIfAbsent(id, name) != null) {
        throw new IllegalStateException("app.workshops has a duplicate id");
      }
    }
    return Collections.unmodifiableMap(catalogue);
  }
}
