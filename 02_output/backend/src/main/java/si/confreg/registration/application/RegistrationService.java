package si.confreg.registration.application;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.domain.FeeCalculator;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationData;
import si.confreg.registration.domain.RegistrationNumberGenerator;
import si.confreg.registration.domain.RegistrationValidator;
import si.confreg.registration.persistence.RegistrationEntity;
import si.confreg.registration.persistence.RegistrationRepository;

/** Registers participants (US-001) and reads stored registrations for the organizer. */
@Service
public class RegistrationService {

  private static final int NUMBER_ATTEMPTS = 3;

  private final RegistrationValidator validator;
  private final FeeCalculator feeCalculator;
  private final RegistrationNumberGenerator numbers;
  private final RegistrationRepository repository;
  private final ConferenceClock clock;
  private final ApplicationEventPublisher events;

  public RegistrationService(
      RegistrationValidator validator,
      FeeCalculator feeCalculator,
      RegistrationNumberGenerator numbers,
      RegistrationRepository repository,
      ConferenceClock clock,
      ApplicationEventPublisher events) {
    this.validator = validator;
    this.feeCalculator = feeCalculator;
    this.numbers = numbers;
    this.repository = repository;
    this.clock = clock;
    this.events = events;
  }

  /**
   * Validates and stores one registration with the fee for the current submission time; the
   * confirmation e-mail is sent after commit.
   *
   * @throws si.confreg.registration.domain.ValidationFailedException if the request is invalid
   */
  @Transactional
  public Registration register(Map<String, Object> request) {
    RegistrationData data = validator.validate(request);
    Instant submissionTime = clock.now();
    Fees fees = feeCalculator.feesAt(submissionTime);
    RegistrationEntity stored =
        repository.save(new RegistrationEntity(unusedNumber(), data, fees, submissionTime));
    events.publishEvent(new RegistrationStored(stored.id()));
    return stored.toRegistration();
  }

  @Transactional(readOnly = true)
  public Optional<Registration> find(String registrationNumber) {
    return repository
        .findByRegistrationNumber(registrationNumber)
        .map(RegistrationEntity::toRegistration);
  }

  private String unusedNumber() {
    for (int attempt = 0; attempt < NUMBER_ATTEMPTS; attempt++) {
      String candidate = numbers.next();
      if (!repository.existsByRegistrationNumber(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("No unused registration number found");
  }
}
