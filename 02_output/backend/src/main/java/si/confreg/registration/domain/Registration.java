package si.confreg.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/** A stored conference registration (docs/02_contracts/registration-storage.sql). */
@Entity
@Table(name = "registration")
public class Registration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "registration_number", nullable = false, updatable = false)
  private String registrationNumber;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "email_normalized", nullable = false)
  private String emailNormalized;

  @Convert(converter = PayerTypeConverter.class)
  @Column(name = "payer_type", nullable = false)
  private PayerType payerType;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "company_address")
  private String companyAddress;

  @Column(name = "company_vat_id")
  private String companyVatId;

  @Column(name = "workshop")
  private String workshop;

  @Column(name = "student", nullable = false)
  private boolean student;

  @Column(name = "net_fee", nullable = false)
  private BigDecimal netFee;

  @Column(name = "vat", nullable = false)
  private BigDecimal vat;

  @Column(name = "gross_fee", nullable = false)
  private BigDecimal grossFee;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @Column(name = "confirmation_sent_at")
  private Instant confirmationSentAt;

  @Column(name = "confirmation_attempts", nullable = false)
  private int confirmationAttempts;

  protected Registration() {
    // for JPA
  }

  /** Data of a new registration, already validated and priced. */
  public record NewRegistration(
      String firstName,
      String lastName,
      String email,
      PayerType payerType,
      String companyName,
      String companyAddress,
      String companyVatId,
      String workshop,
      boolean student) {}

  public static Registration create(
      String registrationNumber, NewRegistration data, Price price, Instant registeredAt) {
    Registration registration = new Registration();
    registration.registrationNumber = registrationNumber;
    registration.firstName = data.firstName();
    registration.lastName = data.lastName();
    registration.email = data.email();
    registration.emailNormalized = normalizeEmail(data.email());
    registration.payerType = data.payerType();
    registration.companyName = data.companyName();
    registration.companyAddress = data.companyAddress();
    registration.companyVatId = data.companyVatId();
    registration.workshop = data.workshop();
    registration.student = data.student();
    registration.netFee = price.netFee();
    registration.vat = price.vat();
    registration.grossFee = price.grossFee();
    registration.registeredAt = registeredAt;
    return registration;
  }

  /** Key of the one-registration-per-address rule (D-12). */
  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public void markConfirmationSent(Instant sentAt) {
    this.confirmationSentAt = sentAt;
    this.confirmationAttempts++;
  }

  public void markConfirmationFailed() {
    this.confirmationAttempts++;
  }

  public Long getId() {
    return id;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public PayerType getPayerType() {
    return payerType;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getCompanyAddress() {
    return companyAddress;
  }

  public String getCompanyVatId() {
    return companyVatId;
  }

  public String getWorkshop() {
    return workshop;
  }

  public boolean isStudent() {
    return student;
  }

  public BigDecimal getNetFee() {
    return netFee;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getGrossFee() {
    return grossFee;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public Instant getConfirmationSentAt() {
    return confirmationSentAt;
  }

  public int getConfirmationAttempts() {
    return confirmationAttempts;
  }
}
