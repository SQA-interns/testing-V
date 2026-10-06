package si.confreg.registration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import si.confreg.registration.domain.Fee;
import si.confreg.registration.domain.Payer;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

/** Row of table {@code registration} (contract: registration-storage.sql). */
@Entity
@Table(name = "registration")
class RegistrationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "registration_number", nullable = false, updatable = false, length = 14)
  private String registrationNumber;

  @Column(name = "status", nullable = false, updatable = false, length = 20)
  private String status;

  @Column(name = "first_name", nullable = false, updatable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, updatable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, updatable = false, length = 254)
  private String email;

  @Column(name = "payer_type", nullable = false, updatable = false, length = 10)
  private String payerType;

  @Column(name = "company_name", updatable = false, length = 200)
  private String companyName;

  @Column(name = "company_address", updatable = false, length = 300)
  private String companyAddress;

  @Column(name = "company_vat_id", updatable = false, length = 32)
  private String companyVatId;

  @Column(name = "workshop", updatable = false, length = 20)
  private String workshop;

  @Column(name = "net_fee", nullable = false, updatable = false, precision = 10, scale = 2)
  private BigDecimal netFee;

  @Column(name = "vat", nullable = false, updatable = false, precision = 10, scale = 2)
  private BigDecimal vat;

  @Column(name = "gross_fee", nullable = false, updatable = false, precision = 10, scale = 2)
  private BigDecimal grossFee;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  protected RegistrationEntity() {
    // for JPA
  }

  static RegistrationEntity from(Registration registration) {
    RegistrationEntity entity = new RegistrationEntity();
    entity.registrationNumber = registration.registrationNumber();
    entity.status = registration.status();
    entity.firstName = registration.firstName();
    entity.lastName = registration.lastName();
    entity.email = registration.email();
    Payer payer = registration.payer();
    entity.payerType = payer.type().code();
    entity.companyName = payer.companyName();
    entity.companyAddress = payer.companyAddress();
    entity.companyVatId = payer.companyVatId();
    entity.workshop = registration.workshopId();
    entity.netFee = registration.fee().net();
    entity.vat = registration.fee().vat();
    entity.grossFee = registration.fee().gross();
    entity.submittedAt = registration.submittedAt();
    return entity;
  }

  Registration toDomain() {
    PayerType type =
        PayerType.fromCode(payerType)
            .orElseThrow(() -> new IllegalStateException("unknown payer type in storage"));
    Payer payer =
        type == PayerType.COMPANY
            ? Payer.company(companyName, companyAddress, companyVatId)
            : Payer.privatePerson();
    return new Registration(
        registrationNumber,
        firstName,
        lastName,
        email,
        payer,
        workshop,
        new Fee(netFee, vat, grossFee),
        submittedAt);
  }
}
