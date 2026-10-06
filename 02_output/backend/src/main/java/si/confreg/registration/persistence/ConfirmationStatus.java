package si.confreg.registration.persistence;

/** Values of {@code registration.confirmation_status} (D-12). */
public final class ConfirmationStatus {

  public static final String PENDING = "pending";
  public static final String SENT = "sent";
  public static final String FAILED = "failed";

  private ConfirmationStatus() {}
}
