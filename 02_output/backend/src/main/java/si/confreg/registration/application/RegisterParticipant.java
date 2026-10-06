package si.confreg.registration.application;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.confreg.registration.domain.Fee;
import si.confreg.registration.domain.FeePolicy;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationNumbers;

/**
 * Use case: submit a registration (AC-001-01 to AC-001-09). Validate, take the submission time
 * once, compute the fee, store, then send one confirmation (spec 5.1, 9).
 */
public final class RegisterParticipant {

  static final int MAX_NUMBER_ATTEMPTS = 3;

  private static final Logger LOG = LoggerFactory.getLogger(RegisterParticipant.class);

  private final RegistrationValidator validator;
  private final FeePolicy feePolicy;
  private final RegistrationNumbers numbers;
  private final RegistrationStore store;
  private final ConfirmationSender confirmations;
  private final TimeSource time;

  public RegisterParticipant(
      RegistrationValidator validator,
      FeePolicy feePolicy,
      RegistrationNumbers numbers,
      RegistrationStore store,
      ConfirmationSender confirmations,
      TimeSource time) {
    this.validator = validator;
    this.feePolicy = feePolicy;
    this.numbers = numbers;
    this.store = store;
    this.confirmations = confirmations;
    this.time = time;
  }

  /** Result of a submission: the stored registration, or the field errors. */
  public sealed interface Outcome permits Registered, Rejected {}

  public record Registered(Registration registration) implements Outcome {}

  public record Rejected(Map<String, String> errors) implements Outcome {

    public Rejected {
      errors = Map.copyOf(errors);
    }
  }

  public Outcome register(RegistrationRequest request) {
    RegistrationValidator.Result result = validator.validate(request);
    if (!result.isValid()) {
      return new Rejected(result.errors());
    }
    RegistrationValidator.ValidRegistration valid = result.valid();
    Instant submittedAt = time.now();
    Fee fee = feePolicy.feeAt(submittedAt);
    Registration registration =
        new Registration(
            numbers.next(),
            valid.firstName(),
            valid.lastName(),
            valid.email(),
            valid.payer(),
            valid.workshopId(),
            fee,
            submittedAt);
    Registration stored = storeWithUniqueNumber(registration);
    sendConfirmation(stored);
    return new Registered(stored);
  }

  private Registration storeWithUniqueNumber(Registration registration) {
    Registration candidate = registration;
    for (int attempt = 1; ; attempt++) {
      try {
        return store.add(candidate);
      } catch (DuplicateRegistrationNumberException e) {
        if (attempt >= MAX_NUMBER_ATTEMPTS) {
          throw e;
        }
        candidate = candidate.withRegistrationNumber(numbers.next());
      }
    }
  }

  /** D-09: a failed e-mail leaves the registration stored; only the number is logged (SR-01). */
  private void sendConfirmation(Registration registration) {
    try {
      confirmations.sendConfirmation(registration);
    } catch (RuntimeException e) {
      LOG.warn(
          "confirmation e-mail failed for {}: {}",
          registration.registrationNumber(),
          e.getClass().getName());
    }
  }
}
