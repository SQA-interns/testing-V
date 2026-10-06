package si.confreg.registration.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationInput;
import si.confreg.registration.domain.RegistrationValidator;

/**
 * Registers a participant (US-001): validate, price by submission time, store and send the
 * confirmation in one transaction, so either both happen or neither (D-25).
 */
@Service
public class RegisterParticipant {

  private final BusinessSettings settings;
  private final RegistrationValidator validator;
  private final RegistrationStore store;
  private final ConfirmationSender sender;
  private final TimeSource time;

  public RegisterParticipant(
      BusinessSettings settings,
      RegistrationStore store,
      ConfirmationSender sender,
      TimeSource time) {
    this.settings = settings;
    this.validator = new RegistrationValidator(settings.workshops());
    this.store = store;
    this.sender = sender;
    this.time = time;
  }

  /**
   * @param unacceptedFields fields the API layer already rejected (unknown or wrongly typed)
   */
  @Transactional
  public Registration register(RegistrationInput input, List<String> unacceptedFields) {
    RegistrationValidator.Result result = validator.validate(input);
    if (!result.valid() || !unacceptedFields.isEmpty()) {
      List<String> invalid = new ArrayList<>(unacceptedFields);
      result.invalidFields().stream().filter(f -> !invalid.contains(f)).forEach(invalid::add);
      throw new RegistrationRejectedException(invalid);
    }
    Instant now = time.now();
    Fees fees = Fees.fromNet(settings.feeSchedule().netFeeAt(now), settings.vatRate());
    Registration registration =
        new Registration(store.nextRegistrationNumber(), result.participant(), fees, now);
    store.save(registration);
    sender.send(registration);
    return registration;
  }
}
