package si.confreg.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A stored registration; table {@code registration} (docs/02_contracts/registration-storage.sql).
 */
@Entity
@Table(name = "registration")
public class Registration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "registration_number", nullable = false, unique = true, length = 20)
  private String registrationNumber;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, length = 254)
  private String email;

  @Column(name = "payer_type", nullable = false, length = 10)
  private String payerType;

  @Column(name = "company_name", length = 200)
  private String companyName;

  @Column(name = "company_address", length = 500)
  private String companyAddress;

  @Column(name = "company_vat_id", length = 30)
  private String companyVatId;

  @Column(name = "workshop", length = 20)
  private String workshop;

  @Column(name = "net_fee", nullable = false, precision = 10, scale = 2)
  private BigDecimal netFee;

  @Column(name = "vat", nullable = false, precision = 10, scale = 2)
  private BigDecimal vat;

  @Column(name = "gross_fee", nullable = false, precision = 10, scale = 2)
  private BigDecimal grossFee;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Registration() {
    // for JPA
  }

  /** A new registration from a validated input; company fields are kept only for company payers. */
  public static Registration create(
      String registrationNumber, RegistrationInput input, Price price, Instant createdAt) {
    Registration registration = new Registration();
    registration.registrationNumber = registrationNumber;
    registration.firstName = input.firstName();
    registration.lastName = input.lastName();
    registration.email = input.email();
    PayerType payerType = PayerType.fromValue(input.payerType()).orElseThrow();
    registration.payerType = payerType.value();
    if (payerType == PayerType.COMPANY) {
      registration.companyName = input.companyName();
      registration.companyAddress = input.companyAddress();
      registration.companyVatId = input.companyVatId();
    }
    registration.workshop = input.workshops().isEmpty() ? null : input.workshops().get(0);
    registration.netFee = price.netFee();
    registration.vat = price.vat();
    registration.grossFee = price.grossFee();
    registration.createdAt = createdAt;
    return registration;
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
    return PayerType.fromValue(payerType).orElseThrow();
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

  public BigDecimal getNetFee() {
    return netFee;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getGrossFee() {
    return grossFee;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
