package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Early-bird or regular net fee by submission time (AC-001-01, AC-001-02). The deadline is a date
 * in the conference time zone and includes the whole day (AR-05).
 */
public record FeeSchedule(
    LocalDate earlyBirdDeadline,
    ZoneId conferenceZone,
    BigDecimal earlyFee,
    BigDecimal regularFee) {

  public BigDecimal netFeeAt(Instant submittedAt) {
    LocalDate submissionDate = submittedAt.atZone(conferenceZone).toLocalDate();
    return submissionDate.isAfter(earlyBirdDeadline) ? regularFee : earlyFee;
  }
}
