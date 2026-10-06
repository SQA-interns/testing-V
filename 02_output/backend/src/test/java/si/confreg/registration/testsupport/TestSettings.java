package si.confreg.registration.testsupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import si.confreg.registration.config.AppSettings;
import si.confreg.registration.config.AppSettings.TestClockMode;

/**
 * Synthetic settings for unit tests. Deliberately different from the 2026 configuration, so the
 * code under test is shown to use whatever is configured (AR-04).
 */
public final class TestSettings {

  public static final ZoneId ZONE = ZoneId.of("America/New_York");
  public static final LocalDate DEADLINE = LocalDate.of(2030, 1, 15);
  public static final BigDecimal FEE_EARLY = new BigDecimal("100.00");
  public static final BigDecimal FEE_REGULAR = new BigDecimal("150.50");
  public static final BigDecimal VAT_RATE = new BigDecimal("0.255");
  public static final String WORKSHOPS = "WA=Alpha workshop;WB=Beta workshop";
  public static final int RATE_LIMIT = 3;
  public static final int MAX_ATTEMPTS = 2;

  private TestSettings() {}

  public static AppSettings settings() {
    return settings(TestClockMode.DISABLED);
  }

  public static AppSettings settings(TestClockMode testClock) {
    return new AppSettings(
        ZONE,
        DEADLINE,
        FEE_EARLY,
        FEE_REGULAR,
        VAT_RATE,
        WORKSHOPS,
        RATE_LIMIT,
        testClock,
        "sender@example.test",
        Duration.ofSeconds(30),
        MAX_ATTEMPTS,
        new AppSettings.Organizer("organizer", "organizer-password-for-tests"));
  }
}
