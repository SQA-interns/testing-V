package si.confreg.registration.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration.NewRegistration;

/** Server-side rules of a registration request (SB-01, D-11, D-13; specification 4.1). */
@Component
public class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int COMPANY_NAME_MAX = 200;
  static final int COMPANY_ADDRESS_MAX = 500;
  static final int VAT_ID_MAX = 30;

  private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
  private static final Pattern CONTROL_EXCEPT_LINE_BREAKS =
      Pattern.compile("[\\p{Cntrl}&&[^\\r\\n]]");
  private static final Pattern EMAIL =
      Pattern.compile("^[^\\s@\\p{Cntrl}]+@[^\\s@\\p{Cntrl}.]+(\\.[^\\s@\\p{Cntrl}.]+)+$");
  private static final Pattern VAT_ID = Pattern.compile("^[A-Za-z0-9 ./-]*$");

  private final AppProperties properties;

  public RegistrationValidator(AppProperties properties) {
    this.properties = properties;
  }

  /** Returns the cleaned registration data or throws with every field error. */
  public NewRegistration validate(RegistrationCommand command) {
    List<FieldError> errors = new ArrayList<>();
    command.typeErrors().stream()
        .sorted()
        .forEach(field -> errors.add(new FieldError(field, "invalid", "Invalid value.")));

    String firstName =
        requiredText(command, "firstName", command.firstName(), NAME_MAX, CONTROL, errors);
    String lastName =
        requiredText(command, "lastName", command.lastName(), NAME_MAX, CONTROL, errors);
    String email = email(command, errors);
    Optional<PayerType> payerType = payerType(command, errors);

    String companyName = clean(command.companyName());
    String companyAddress = clean(command.companyAddress());
    String companyVatId = clean(command.companyVatId());
    if (payerType.orElse(null) == PayerType.COMPANY) {
      companyName =
          requiredText(command, "companyName", companyName, COMPANY_NAME_MAX, CONTROL, errors);
      companyAddress =
          requiredText(
              command,
              "companyAddress",
              companyAddress,
              COMPANY_ADDRESS_MAX,
              CONTROL_EXCEPT_LINE_BREAKS,
              errors);
      companyVatId = vatId(command, companyVatId, errors);
    } else if (payerType.orElse(null) == PayerType.PRIVATE) {
      notAllowed(command, "companyName", companyName, errors);
      notAllowed(command, "companyAddress", companyAddress, errors);
      notAllowed(command, "companyVatId", companyVatId, errors);
      companyName = null;
      companyAddress = null;
      companyVatId = null;
    }
    String workshop = workshop(command, errors);
    boolean student = command.student();

    if (!errors.isEmpty()) {
      throw new ValidationFailedException(errors);
    }
    return new NewRegistration(
        firstName,
        lastName,
        email,
        payerType.orElseThrow(),
        companyName,
        companyAddress,
        companyVatId,
        workshop,
        student);
  }

  private static String clean(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static int length(String value) {
    return value.codePointCount(0, value.length());
  }

  private static String requiredText(
      RegistrationCommand command,
      String field,
      String raw,
      int max,
      Pattern forbidden,
      List<FieldError> errors) {
    if (command.typeErrors().contains(field)) {
      return null;
    }
    String value = clean(raw);
    if (value == null) {
      errors.add(new FieldError(field, "required", "This field is required."));
    } else if (length(value) > max) {
      errors.add(new FieldError(field, "too_long", "At most " + max + " characters."));
    } else if (forbidden.matcher(value).find()) {
      errors.add(new FieldError(field, "invalid", "Contains characters that are not allowed."));
    }
    return value;
  }

  private static String email(RegistrationCommand command, List<FieldError> errors) {
    if (command.typeErrors().contains("email")) {
      return null;
    }
    String value = clean(command.email());
    if (value == null) {
      errors.add(new FieldError("email", "required", "This field is required."));
    } else if (length(value) > EMAIL_MAX) {
      errors.add(new FieldError("email", "too_long", "At most " + EMAIL_MAX + " characters."));
    } else if (!EMAIL.matcher(value).matches()) {
      errors.add(new FieldError("email", "invalid", "Enter a valid e-mail address."));
    }
    return value;
  }

  private static Optional<PayerType> payerType(
      RegistrationCommand command, List<FieldError> errors) {
    if (command.typeErrors().contains("payerType")) {
      return Optional.empty();
    }
    String value = clean(command.payerType());
    if (value == null) {
      errors.add(new FieldError("payerType", "required", "This field is required."));
      return Optional.empty();
    }
    Optional<PayerType> payerType = PayerType.fromApiValue(value);
    if (payerType.isEmpty()) {
      errors.add(new FieldError("payerType", "invalid", "Choose private or company."));
    }
    return payerType;
  }

  private static String vatId(RegistrationCommand command, String value, List<FieldError> errors) {
    if (command.typeErrors().contains("companyVatId") || value == null) {
      return null;
    }
    if (length(value) > VAT_ID_MAX) {
      errors.add(
          new FieldError("companyVatId", "too_long", "At most " + VAT_ID_MAX + " characters."));
    } else if (!VAT_ID.matcher(value).matches()) {
      errors.add(new FieldError("companyVatId", "invalid", "Enter a valid VAT ID."));
    }
    return value;
  }

  private static void notAllowed(
      RegistrationCommand command, String field, String value, List<FieldError> errors) {
    if (value != null && !command.typeErrors().contains(field)) {
      errors.add(new FieldError(field, "not_allowed", "Only allowed when a company pays the fee."));
    }
  }

  private String workshop(RegistrationCommand command, List<FieldError> errors) {
    if (command.typeErrors().contains("workshops") || command.workshops() == null) {
      return null;
    }
    List<String> ids = command.workshops();
    if (ids.isEmpty()) {
      return null;
    }
    if (ids.size() > 1) {
      errors.add(new FieldError("workshops", "too_many", "Choose at most one workshop."));
      return null;
    }
    String id = ids.get(0) == null ? null : ids.get(0).strip();
    if (id == null || !properties.workshops().containsKey(id)) {
      errors.add(new FieldError("workshops", "invalid", "Unknown workshop."));
      return null;
    }
    return id;
  }
}
