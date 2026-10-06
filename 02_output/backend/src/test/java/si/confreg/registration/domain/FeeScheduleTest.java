package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FeeScheduleTest {

  private static final BigDecimal EARLY = new BigDecimal("1.00");
  private static final BigDecimal REGULAR = new BigDecimal("2.00");
  private final FeeSchedule schedule =
      new FeeSchedule(LocalDate.of(2026, 7, 31), ZoneId.of("Europe/Ljubljana"), EARLY, REGULAR);

  @ParameterizedTest
  @CsvSource({
    "2026-01-01T00:00:00Z, 1.00",
    "2026-07-30T22:00:00Z, 1.00", // 31 July 00:00 local (CEST, UTC+2)
    "2026-07-31T21:59:59Z, 1.00", // 31 July 23:59:59 local
    "2026-07-31T22:00:00Z, 2.00", // 1 August 00:00 local, still 31 July in UTC
    "2026-08-01T10:00:00Z, 2.00",
    "2027-01-01T00:00:00Z, 2.00"
  })
  void choosesFeeByDateInConferenceZone(String instant, String expected) {
    assertThat(schedule.netFeeAt(Instant.parse(instant))).isEqualByComparingTo(expected);
  }
}
