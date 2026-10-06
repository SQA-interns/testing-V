package si.confreg.registration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Row of the {@code registration} table (docs/02_contracts/registration-storage.sql). */
@Entity
@Table(name = "registration")
class RegistrationEntity {

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

  @Column(name = "company_vat_id", length = 20)
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

  protected RegistrationEntity() {
    // for JPA
  }

  @SuppressWarnings("PMD.ExcessiveParameterList")
  RegistrationEntity(
      String registrationNumber,
      String firstName,
      String lastName,
      String email,
      String payerType,
      String companyName,
      String companyAddress,
      String companyVatId,
      String workshop,
      BigDecimal netFee,
      BigDecimal vat,
      BigDecimal grossFee,
      Instant createdAt) {
    this.registrationNumber = registrationNumber;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.payerType = payerType;
    this.companyName = companyName;
    this.companyAddress = companyAddress;
    this.companyVatId = companyVatId;
    this.workshop = workshop;
    this.netFee = netFee;
    this.vat = vat;
    this.grossFee = grossFee;
    this.createdAt = createdAt;
  }

  String registrationNumber() {
    return registrationNumber;
  }

  String firstName() {
    return firstName;
  }

  String lastName() {
    return lastName;
  }

  String email() {
    return email;
  }

  String payerType() {
    return payerType;
  }

  String companyName() {
    return companyName;
  }

  String companyAddress() {
    return companyAddress;
  }

  String companyVatId() {
    return companyVatId;
  }

  String workshop() {
    return workshop;
  }

  BigDecimal netFee() {
    return netFee;
  }

  BigDecimal vat() {
    return vat;
  }

  BigDecimal grossFee() {
    return grossFee;
  }

  Instant createdAt() {
    return createdAt;
  }
}
