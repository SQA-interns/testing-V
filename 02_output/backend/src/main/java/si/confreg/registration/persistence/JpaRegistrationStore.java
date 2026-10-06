package si.confreg.registration.persistence;

import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.RegistrationStore;
import si.confreg.registration.domain.Company;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Participant;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

/** JPA adapter of {@link RegistrationStore} (AR-06; parameterised queries only, SB-05). */
@Component
class JpaRegistrationStore implements RegistrationStore {

  private final RegistrationJpaRepository repository;

  JpaRegistrationStore(RegistrationJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public String nextRegistrationNumber() {
    return String.format(Locale.ROOT, "REG-%06d", repository.nextRegistrationSequence());
  }

  @Override
  public void save(Registration registration) {
    Participant p = registration.participant();
    Company company = p.company();
    Fees fees = registration.fees();
    repository.saveAndFlush(
        new RegistrationEntity(
            registration.registrationNumber(),
            p.firstName(),
            p.lastName(),
            p.email(),
            p.payerType().value(),
            company == null ? null : company.name(),
            company == null ? null : company.address(),
            company == null ? null : company.vatId(),
            p.workshopId(),
            fees.netFee(),
            fees.vat(),
            fees.grossFee(),
            registration.createdAt()));
  }

  @Override
  public Optional<Registration> find(String registrationNumber) {
    return repository
        .findByRegistrationNumber(registrationNumber)
        .map(JpaRegistrationStore::toDomain);
  }

  private static Registration toDomain(RegistrationEntity e) {
    PayerType payerType = PayerType.fromValue(e.payerType()).orElseThrow();
    Company company =
        payerType == PayerType.COMPANY
            ? new Company(e.companyName(), e.companyAddress(), e.companyVatId())
            : null;
    Participant participant =
        new Participant(e.firstName(), e.lastName(), e.email(), payerType, company, e.workshop());
    return new Registration(
        e.registrationNumber(),
        participant,
        new Fees(e.netFee(), e.vat(), e.grossFee()),
        e.createdAt());
  }
}
