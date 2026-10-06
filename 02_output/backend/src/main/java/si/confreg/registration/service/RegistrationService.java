package si.confreg.registration.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.config.AppProperties;
import si.confreg.registration.domain.FieldError;
import si.confreg.registration.domain.PricingPolicy;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationInput;
import si.confreg.registration.domain.RegistrationValidator;
import si.confreg.registration.mail.ConfirmationMailer;
import si.confreg.registration.mail.ConfirmationNotSentException;
import si.confreg.registration.persistence.RegistrationRepository;
import si.confreg.registration.time.AppClock;

/**
 * Registration use case (docs/02_specification.md 3.1): validate, price, number, store and confirm
 * by e-mail in one transaction. If the e-mail is not accepted, nothing is stored (D-22).
 */
@Service
public class RegistrationService {

  private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationRepository repository;
  private final ConfirmationMailer mailer;
  private final AppClock clock;
  private final RegistrationValidator validator;
  private final PricingPolicy pricing;

  public RegistrationService(
      RegistrationRepository repository,
      ConfirmationMailer mailer,
      AppClock clock,
      AppProperties properties) {
    this.repository = repository;
    this.mailer = mailer;
    this.clock = clock;
    this.validator = new RegistrationValidator(properties.workshopCatalogue().keySet());
    this.pricing =
        new PricingPolicy(
            properties.conferenceZone(),
            properties.earlyBirdDeadlineDate(),
            properties.feeEarly(),
            properties.feeRegular(),
            properties.vatRate());
  }

  /**
   * Validates, stores and confirms a registration.
   *
   * @throws InvalidRegistrationException with every violation, when the input is invalid
   * @throws RegistrationNotCompletedException when the e-mail fails; the transaction is rolled back
   */
  @Transactional
  public Registration register(RegistrationInput input) {
    List<FieldError> errors = validator.validate(input);
    if (!errors.isEmpty()) {
      LOG.info(
          "Registration rejected; invalid fields: {}",
          errors.stream().map(FieldError::field).distinct().toList());
      throw new InvalidRegistrationException(errors);
    }
    Instant now = clock.now();
    String number = String.format(Locale.ROOT, "REG-%06d", repository.nextRegistrationSequence());
    Registration registration =
        repository.saveAndFlush(Registration.create(number, input, pricing.priceAt(now), now));
    try {
      mailer.sendConfirmation(registration);
    } catch (ConfirmationNotSentException e) {
      LOG.warn(
          "Registration {} rolled back: confirmation e-mail not sent ({})",
          number,
          e.getCause() == null ? "unknown" : e.getCause().getClass().getSimpleName());
      throw new RegistrationNotCompletedException(e);
    }
    LOG.info("Registration {} stored and confirmed", number);
    return registration;
  }

  @Transactional(readOnly = true)
  public Optional<Registration> find(String registrationNumber) {
    return repository.findByRegistrationNumber(registrationNumber);
  }
}
