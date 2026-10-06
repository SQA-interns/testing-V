package si.confreg.registration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationData;

/** Row of table {@code registration} (storage contract, Flyway V1). */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  static final String STATUS_REGISTERED = "registered";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "registration_number", nullable = false, length = 32, updatable = false)
  private String registrationNumber;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "payer_type", nullable = false, length = 16)
  private String payerType;

  @Column(name = "company_name", length = 200)
  private String companyName;

  @Column(name = "company_address", length = 500)
  private String companyAddress;

  @Column(name = "company_vat_id", length = 30)
  private String companyVatId;

  @Column(length = 32)
  private String workshop;

  @Column(name = "net_fee", nullable = false, precision = 10, scale = 2)
  private BigDecimal netFee;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal vat;

  @Column(name = "gross_fee", nullable = false, precision = 10, scale = 2)
  private BigDecimal grossFee;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "confirmation_status", nullable = false, length = 16)
  private String confirmationStatus;

  @Column(name = "confirmation_attempts", nullable = false)
  private int confirmationAttempts;

  @Column(name = "confirmation_sent_at")
  private Instant confirmationSentAt;

  protected RegistrationEntity() {
    // for JPA
  }

  public RegistrationEntity(
      String registrationNumber, RegistrationData data, Fees fees, Instant submittedAt) {
    this.registrationNumber = registrationNumber;
    this.status = STATUS_REGISTERED;
    this.firstName = data.firstName();
    this.lastName = data.lastName();
    this.email = data.email();
    this.payerType = data.payerType().value();
    this.companyName = data.companyName();
    this.companyAddress = data.companyAddress();
    this.companyVatId = data.companyVatId();
    this.workshop = data.workshop();
    this.netFee = fees.netFee();
    this.vat = fees.vat();
    this.grossFee = fees.grossFee();
    this.submittedAt = submittedAt;
    this.confirmationStatus = ConfirmationStatus.PENDING;
    this.confirmationAttempts = 0;
  }

  public Long id() {
    return id;
  }

  public int confirmationAttempts() {
    return confirmationAttempts;
  }

  public String confirmationStatus() {
    return confirmationStatus;
  }

  public Registration toRegistration() {
    return new Registration(
        registrationNumber,
        firstName,
        lastName,
        email,
        PayerType.fromValue(payerType).orElseThrow(),
        companyName,
        companyAddress,
        companyVatId,
        workshop,
        netFee,
        vat,
        grossFee);
  }
}
