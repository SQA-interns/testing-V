package si.confreg.registration.time;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * The only source of "now" for business logic (AR-05). Returns the test instant set by {@link
 * TestClockFilter} for the current request, otherwise the system time in UTC.
 */
@Component
public class AppClock {

  static final String TEST_NOW_ATTRIBUTE = AppClock.class.getName() + ".testNow";

  private final Clock clock;

  public AppClock(Clock clock) {
    this.clock = clock;
  }

  public Instant now() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null
        && attributes.getAttribute(TEST_NOW_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
            instanceof Instant testNow) {
      return testNow;
    }
    return clock.instant();
  }
}
