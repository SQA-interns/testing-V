package si.confreg.registration.application;

import java.time.Instant;

/** The single source of the current time (AR-05); honours the test clock for a request. */
public interface TimeSource {

  Instant now();
}
