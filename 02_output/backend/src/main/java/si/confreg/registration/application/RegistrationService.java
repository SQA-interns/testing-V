package si.confreg.registration.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.domain.Price;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.Registration.NewRegistration;
import si.confreg.registration.domain.RegistrationRepository;

/** Registers a participant (US-001; specification 4.2). */
@Service
public class RegistrationService {

  private static final String EMAIL_CONSTRAINT = "uk_registration_email";

  private final RegistrationValidator validator;
  private final PricingService pricing;
  private final RegistrationRepository repository;
  private final TimeSource timeSource;
  private final ApplicationEventPublisher events;

  public RegistrationService(
      RegistrationValidator validator,
      PricingService pricing,
      RegistrationRepository repository,
      TimeSource timeSource,
      ApplicationEventPublisher events) {
    this.validator = validator;
    this.pricing = pricing;
    this.repository = repository;
    this.timeSource = timeSource;
    this.events = events;
  }

  @Transactional
  public Registration register(RegistrationCommand command) {
    NewRegistration data = validator.validate(command);
    // PostgreSQL stores microseconds; the answer must equal what is stored.
    Instant now = timeSource.now().truncatedTo(ChronoUnit.MICROS);
    Price price = pricing.price(data.student(), now);
    if (repository.existsByEmailNormalized(Registration.normalizeEmail(data.email()))) {
      throw new DuplicateRegistrationException();
    }
    String number = String.format(Locale.ROOT, "CR-%06d", repository.nextRegistrationSequence());
    Registration saved;
    try {
      saved = repository.saveAndFlush(Registration.create(number, data, price, now));
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateEmail(e)) {
        throw new DuplicateRegistrationException();
      }
      throw e;
    }
    events.publishEvent(new RegistrationCreatedEvent(saved.getId()));
    return saved;
  }

  private static boolean isDuplicateEmail(DataIntegrityViolationException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      String message = cause.getMessage();
      if (message != null && message.contains(EMAIL_CONSTRAINT)) {
        return true;
      }
    }
    return false;
  }
}
