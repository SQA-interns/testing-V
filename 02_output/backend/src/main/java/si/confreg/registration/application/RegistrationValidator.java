package si.confreg.registration.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import si.confreg.registration.domain.Payer;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.WorkshopCatalog;

/**
 * Server-side validation of a registration request (AC-001-07, D-08, SB-01, SR-05). Collects at
 * most one error per field, in the order required, type, length, characters, format.
 */
public final class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int COMPANY_NAME_MAX = 200;
  static final int COMPANY_ADDRESS_MAX = 300;
  static final int VAT_ID_MAX = 32;

  static final String FIRST_NAME = "firstName";
  static final String LAST_NAME = "lastName";
  static final String EMAIL = "email";
  static final String PAYER_TYPE = "payerType";
  static final String COMPANY_NAME = "companyName";
  static final String COMPANY_ADDRESS = "companyAddress";
  static final String COMPANY_VAT_ID = "companyVatId";
  static final String WORKSHOPS = "workshops";

  static final String REQUIRED = "is required";
  static final String WRONG_TYPE = "has the wrong type";
  static final String CONTROL_CHARACTERS = "must not contain control characters";
  static final String INVALID_EMAIL = "must be a valid e-mail address";
  static final String INVALID_PAYER_TYPE = "must be 'private' or 'company'";
  static final String REQUIRED_FOR_COMPANY = "is required for a company payer";
  static final String TOO_MANY_WORKSHOPS = "select at most one workshop";
  static final String UNKNOWN_WORKSHOP = "is not an offered workshop";

  private static final Pattern EMAIL_FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final WorkshopCatalog workshops;

  public RegistrationValidator(WorkshopCatalog workshops) {
    this.workshops = workshops;
  }

  /** Normalised, valid input. */
  public record ValidRegistration(
      String firstName, String lastName, String email, Payer payer, String workshopId) {}

  /** Either a valid registration or the field errors. */
  public record Result(ValidRegistration valid, Map<String, String> errors) {

    public boolean isValid() {
      return errors.isEmpty();
    }
  }

  public Result validate(RegistrationRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    String firstName = text(request, FIRST_NAME, request.firstName(), NAME_MAX, true, errors);
    String lastName = text(request, LAST_NAME, request.lastName(), NAME_MAX, true, errors);
    String email = text(request, EMAIL, request.email(), EMAIL_MAX, true, errors);
    if (email != null && !EMAIL_FORMAT.matcher(email).matches()) {
      errors.put(EMAIL, INVALID_EMAIL);
      email = null;
    }
    Optional<PayerType> payerType = payerType(request, errors);
    Payer payer = payerType.map(type -> payer(request, type, errors)).orElse(null);
    String workshopId = workshop(request, errors);

    if (!errors.isEmpty()) {
      return new Result(null, Map.copyOf(errors));
    }
    return new Result(
        new ValidRegistration(firstName, lastName, email, payer, workshopId), Map.of());
  }

  private Optional<PayerType> payerType(RegistrationRequest request, Map<String, String> errors) {
    if (request.wrongTypeFields().contains(PAYER_TYPE)) {
      errors.put(PAYER_TYPE, WRONG_TYPE);
      return Optional.empty();
    }
    if (request.payerType() == null || request.payerType().isBlank()) {
      errors.put(PAYER_TYPE, REQUIRED);
      return Optional.empty();
    }
    Optional<PayerType> type = PayerType.fromCode(request.payerType());
    if (type.isEmpty()) {
      errors.put(PAYER_TYPE, INVALID_PAYER_TYPE);
    }
    return type;
  }

  private Payer payer(RegistrationRequest request, PayerType type, Map<String, String> errors) {
    if (type == PayerType.PRIVATE) {
      return Payer.privatePerson();
    }
    String name =
        text(request, COMPANY_NAME, request.companyName(), COMPANY_NAME_MAX, false, errors);
    String address =
        text(
            request, COMPANY_ADDRESS, request.companyAddress(), COMPANY_ADDRESS_MAX, false, errors);
    String vatId = text(request, COMPANY_VAT_ID, request.companyVatId(), VAT_ID_MAX, false, errors);
    if (name == null || address == null || vatId == null) {
      return null;
    }
    return Payer.company(name, address, vatId);
  }

  private String workshop(RegistrationRequest request, Map<String, String> errors) {
    if (request.wrongTypeFields().contains(WORKSHOPS)) {
      errors.put(WORKSHOPS, WRONG_TYPE);
      return null;
    }
    List<String> selected = request.workshops();
    if (selected == null || selected.isEmpty()) {
      return null;
    }
    if (selected.size() > 1) {
      errors.put(WORKSHOPS, TOO_MANY_WORKSHOPS);
      return null;
    }
    String id = selected.get(0);
    if (id == null || !workshops.contains(id)) {
      errors.put(WORKSHOPS, UNKNOWN_WORKSHOP);
      return null;
    }
    return id;
  }

  /**
   * Checks one text field; returns the trimmed value, or {@code null} after recording an error.
   *
   * @param participantField {@code true} for participant fields (message "is required")
   */
  private static String text(
      RegistrationRequest request,
      String field,
      String raw,
      int maxLength,
      boolean participantField,
      Map<String, String> errors) {
    if (request.wrongTypeFields().contains(field)) {
      errors.put(field, WRONG_TYPE);
      return null;
    }
    String value = raw == null ? "" : raw.strip();
    if (value.isEmpty()) {
      errors.put(field, participantField ? REQUIRED : REQUIRED_FOR_COMPANY);
      return null;
    }
    if (value.codePointCount(0, value.length()) > maxLength) {
      errors.put(field, "must be at most " + maxLength + " characters");
      return null;
    }
    if (value.chars().anyMatch(c -> Character.getType(c) == Character.CONTROL)) {
      errors.put(field, CONTROL_CHARACTERS);
      return null;
    }
    return value;
  }
}
