package si.confreg.registration.infrastructure.clock;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.TimeSource;

/**
 * System clock, replaced for the duration of one request by the instant of its {@code X-Test-Now}
 * header when the test clock is enabled (AR-05, set by {@link TestClockFilter}).
 */
@Component
public class RequestTimeSource implements TimeSource {

  private static final ThreadLocal<Instant> REQUEST_NOW = new ThreadLocal<>();

  private final Clock clock = Clock.systemUTC();

  @Override
  public Instant now() {
    Instant fixed = REQUEST_NOW.get();
    return fixed != null ? fixed : clock.instant();
  }

  static void setRequestNow(Instant instant) {
    REQUEST_NOW.set(instant);
  }

  static void clearRequestNow() {
    REQUEST_NOW.remove();
  }
}
