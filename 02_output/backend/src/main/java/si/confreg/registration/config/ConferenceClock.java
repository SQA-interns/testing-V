package si.confreg.registration.config;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * The only source of the current time in the backend (AR-05). Returns the request's test instant
 * when the test clock bound one, otherwise the system UTC clock.
 */
@Component
public class ConferenceClock {

  private static final ThreadLocal<Instant> TEST_NOW = new ThreadLocal<>();

  private final Clock system = Clock.systemUTC();

  public Instant now() {
    Instant testNow = TEST_NOW.get();
    return testNow != null ? testNow : system.instant();
  }

  void bindTestNow(Instant instant) {
    TEST_NOW.set(instant);
  }

  void clearTestNow() {
    TEST_NOW.remove();
  }
}
