package si.confreg.registration.application;

import java.time.Instant;

/** The single source of the current time (AR-05). */
public interface TimeSource {

  /** Business time; honours the test clock when it is enabled. */
  Instant now();

  /** Real system time, never affected by the test clock (for security windows). */
  Instant systemNow();
}
