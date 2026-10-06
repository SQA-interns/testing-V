package si.confreg.registration.clock;

/** Whether the per-request test clock is honoured (APP_TEST_CLOCK; never in production, SR-04). */
public record TestClockSettings(boolean enabled) {}
