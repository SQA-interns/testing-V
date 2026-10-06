package si.confreg.registration.acceptance.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Business settings the acceptance tests give the backend (AR-04). They deliberately differ from
 * the defaults in environments.md, so a test only passes when the backend reads its configuration.
 * Expected amounts are derived from these values, never written as literals.
 */
public final class AcceptanceConfig {

  public static final ZoneId CONFERENCE_TZ = ZoneId.of("Europe/Ljubljana");
  public static final LocalDate EARLY_BIRD_DEADLINE = LocalDate.of(2026, 5, 15);
  public static final BigDecimal FEE_EARLY = new BigDecimal("111.11");
  public static final BigDecimal FEE_REGULAR = new BigDecimal("222.26");
  public static final BigDecimal VAT_RATE = new BigDecimal("0.25");
  public static final String WORKSHOP_A = "WA";
  public static final String WORKSHOP_A_TITLE = "Alpha workshop";
  public static final String WORKSHOP_B = "WB";
  public static final String WORKSHOP_B_TITLE = "Beta delavnica čšž";
  public static final String MAIL_FROM = "registration@acceptance.example";

  /** Generated per run, so no credential is written in the repository. */
  public static final String ORGANIZER_USERNAME = "organizer-" + UUID.randomUUID();

  public static final String ORGANIZER_PASSWORD = UUID.randomUUID().toString();

  private AcceptanceConfig() {}

  /** Settings with the environment-variable names of environments.md and the specification. */
  public static Map<String, String> settings() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put("APP_CONFERENCE_TZ", CONFERENCE_TZ.getId());
    settings.put("APP_EARLY_BIRD_DEADLINE", EARLY_BIRD_DEADLINE.toString());
    settings.put("APP_FEE_EARLY", FEE_EARLY.toPlainString());
    settings.put("APP_FEE_REGULAR", FEE_REGULAR.toPlainString());
    settings.put("APP_VAT_RATE", VAT_RATE.toPlainString());
    settings.put(
        "APP_WORKSHOPS",
        WORKSHOP_A + "=" + WORKSHOP_A_TITLE + ";" + WORKSHOP_B + "=" + WORKSHOP_B_TITLE);
    settings.put("APP_RATE_LIMIT_PER_HOUR", "100000");
    settings.put("APP_TEST_CLOCK", "enabled");
    settings.put("APP_MAIL_FROM", MAIL_FROM);
    settings.put("ORGANIZER_USERNAME", ORGANIZER_USERNAME);
    settings.put("ORGANIZER_PASSWORD", ORGANIZER_PASSWORD);
    return settings;
  }

  /** Noon on the deadline day in the conference time zone: early bird. */
  public static Instant earlyBirdTime() {
    return EARLY_BIRD_DEADLINE.atTime(LocalTime.NOON).atZone(CONFERENCE_TZ).toInstant();
  }

  /** Noon ten days after the deadline in the conference time zone: regular. */
  public static Instant regularTime() {
    return EARLY_BIRD_DEADLINE
        .plusDays(10)
        .atTime(LocalTime.NOON)
        .atZone(CONFERENCE_TZ)
        .toInstant();
  }

  /** 23:59:59 local on the deadline day. */
  public static Instant lastEarlyBirdSecond() {
    return EARLY_BIRD_DEADLINE.atTime(23, 59, 59).atZone(CONFERENCE_TZ).toInstant();
  }

  /** 00:00:00 local on the day after the deadline. */
  public static Instant firstRegularInstant() {
    return EARLY_BIRD_DEADLINE.plusDays(1).atStartOfDay(CONFERENCE_TZ).toInstant();
  }

  public static BigDecimal vatOf(BigDecimal net) {
    return net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal grossOf(BigDecimal net) {
    return net.add(vatOf(net)).setScale(2, RoundingMode.UNNECESSARY);
  }
}
