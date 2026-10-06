package si.confreg.registration.clock;

import java.time.Instant;
import si.confreg.registration.application.TimeSource;

/** The single clock component (AR-05). Returns the request's test instant when one is set. */
public class ApplicationTimeSource implements TimeSource {

  private static final ThreadLocal<Instant> TEST_NOW = new ThreadLocal<>();

  private final TestClockSettings settings;

  public ApplicationTimeSource(TestClockSettings settings) {
    this.settings = settings;
  }

  @Override
  public Instant now() {
    Instant testNow = settings.enabled() ? TEST_NOW.get() : null;
    return testNow != null ? testNow : Instant.now();
  }

  @Override
  public Instant systemNow() {
    return Instant.now();
  }

  static void setTestNow(Instant instant) {
    TEST_NOW.set(instant);
  }

  static void clearTestNow() {
    TEST_NOW.remove();
  }
}
