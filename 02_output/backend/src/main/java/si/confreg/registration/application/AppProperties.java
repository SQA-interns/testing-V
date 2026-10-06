package si.confreg.registration.application;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Business and runtime settings (docs/02_specification.md section 3, AR-04). Raw values are parsed
 * once at startup; an invalid value stops startup with a message naming the setting.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final ZoneId conferenceTz;
  private final LocalDate earlyBirdDeadline;
  private final BigDecimal feeEarly;
  private final BigDecimal feeRegular;
  private final BigDecimal vatRate;
  private final Map<String, String> workshops;
  private final int rateLimitPerHour;
  private final boolean testClockEnabled;
  private final String mailFrom;
  private final String smtpHost;
  private final int smtpPort;
  private final boolean smtpTls;

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public AppProperties(
      String conferenceTz,
      String earlyBirdDeadline,
      String feeEarly,
      String feeRegular,
      String vatRate,
      String workshops,
      String rateLimitPerHour,
      String testClock,
      String mailFrom,
      String smtpHost,
      String smtpPort,
      String smtpTls) {
    this.conferenceTz = zone(conferenceTz);
    this.earlyBirdDeadline = date(earlyBirdDeadline);
    this.feeEarly = amount("APP_FEE_EARLY", feeEarly);
    this.feeRegular = amount("APP_FEE_REGULAR", feeRegular);
    this.vatRate = amount("APP_VAT_RATE", vatRate);
    this.workshops = parseWorkshops(workshops);
    this.rateLimitPerHour = positiveInt("APP_RATE_LIMIT_PER_HOUR", rateLimitPerHour);
    this.testClockEnabled = enabledFlag(testClock);
    this.mailFrom = required("APP_MAIL_FROM", mailFrom);
    this.smtpHost = required("APP_SMTP_HOST", smtpHost);
    this.smtpPort = positiveInt("APP_SMTP_PORT", smtpPort);
    this.smtpTls = bool("APP_SMTP_TLS", smtpTls);
  }

  /** Parses {@code id=title;id=title}; ids are unique and non-empty. */
  static Map<String, String> parseWorkshops(String raw) {
    Map<String, String> result = new LinkedHashMap<>();
    if (raw == null || raw.isBlank()) {
      return Collections.unmodifiableMap(result);
    }
    for (String entry : raw.split(";")) {
      if (entry.isBlank()) {
        continue;
      }
      int separator = entry.indexOf('=');
      String id = separator < 0 ? "" : entry.substring(0, separator).trim();
      String title = separator < 0 ? "" : entry.substring(separator + 1).trim();
      if (id.isEmpty() || title.isEmpty() || result.containsKey(id)) {
        throw invalid("APP_WORKSHOPS");
      }
      result.put(id, title);
    }
    return Collections.unmodifiableMap(result);
  }

  private static ZoneId zone(String value) {
    try {
      return ZoneId.of(required("APP_CONFERENCE_TZ", value).trim());
    } catch (DateTimeException e) {
      throw invalid("APP_CONFERENCE_TZ");
    }
  }

  private static LocalDate date(String value) {
    try {
      return LocalDate.parse(required("APP_EARLY_BIRD_DEADLINE", value).trim());
    } catch (DateTimeParseException e) {
      throw invalid("APP_EARLY_BIRD_DEADLINE");
    }
  }

  private static BigDecimal amount(String name, String value) {
    try {
      BigDecimal parsed = new BigDecimal(required(name, value).trim());
      if (parsed.signum() < 0) {
        throw invalid(name);
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw invalid(name);
    }
  }

  private static int positiveInt(String name, String value) {
    try {
      int parsed = Integer.parseInt(required(name, value).trim());
      if (parsed <= 0) {
        throw invalid(name);
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw invalid(name);
    }
  }

  private static boolean enabledFlag(String value) {
    String normalized = value == null ? "disabled" : value.trim();
    if ("enabled".equals(normalized)) {
      return true;
    }
    if ("disabled".equals(normalized) || normalized.isEmpty()) {
      return false;
    }
    throw invalid("APP_TEST_CLOCK");
  }

  private static boolean bool(String name, String value) {
    String normalized = value == null ? "" : value.trim();
    if ("true".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized)) {
      return false;
    }
    throw invalid(name);
  }

  private static String required(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required setting " + name);
    }
    return value;
  }

  private static IllegalStateException invalid(String name) {
    return new IllegalStateException("Invalid value for setting " + name);
  }

  public ZoneId conferenceTz() {
    return conferenceTz;
  }

  public LocalDate earlyBirdDeadline() {
    return earlyBirdDeadline;
  }

  public BigDecimal feeEarly() {
    return feeEarly;
  }

  public BigDecimal feeRegular() {
    return feeRegular;
  }

  public BigDecimal vatRate() {
    return vatRate;
  }

  /** Workshop ids to titles, in configuration order. */
  public Map<String, String> workshops() {
    return workshops;
  }

  public List<String> workshopIds() {
    return new ArrayList<>(workshops.keySet());
  }

  public int rateLimitPerHour() {
    return rateLimitPerHour;
  }

  public boolean testClockEnabled() {
    return testClockEnabled;
  }

  public String mailFrom() {
    return mailFrom;
  }

  public String smtpHost() {
    return smtpHost;
  }

  public int smtpPort() {
    return smtpPort;
  }

  public boolean smtpTls() {
    return smtpTls;
  }
}
