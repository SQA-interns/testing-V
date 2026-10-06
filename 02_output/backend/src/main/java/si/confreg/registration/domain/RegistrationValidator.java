package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation rules of a registration (SB-01, D-20, D-21, SR-05): required fields, lengths, e-mail
 * syntax, payer type, company fields by payer type, workshops and control characters.
 */
public final class RegistrationValidator {

  public static final int NAME_MAX = 100;
  public static final int EMAIL_MAX = 254;
  public static final int COMPANY_NAME_MAX = 200;
  public static final int COMPANY_ADDRESS_MAX = 500;
  public static final int VAT_ID_MAX = 30;

  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");
  private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
  private static final Pattern CONTROL_EXCEPT_LINE_BREAK =
      Pattern.compile("[\\p{Cntrl}&&[^\\r\\n]]");

  private final Set<String> workshopIds;

  public RegistrationValidator(Set<String> workshopIds) {
    this.workshopIds = Set.copyOf(workshopIds);
  }

  /** All violations of the input; empty when the registration is valid. */
  public List<FieldError> validate(RegistrationInput input) {
    List<FieldError> errors = new ArrayList<>();
    text(errors, "firstName", input.firstName(), true, NAME_MAX, CONTROL);
    text(errors, "lastName", input.lastName(), true, NAME_MAX, CONTROL);
    text(errors, "email", input.email(), true, EMAIL_MAX, CONTROL);
    if (input.email() != null
        && input.email().length() <= EMAIL_MAX
        && !EMAIL.matcher(input.email()).matches()) {
      errors.add(new FieldError("email", "must be a valid e-mail address"));
    }
    if (input.payerType() == null) {
      errors.add(new FieldError("payerType", "is required"));
    } else if (PayerType.fromValue(input.payerType()).isEmpty()) {
      errors.add(new FieldError("payerType", "must be 'private' or 'company'"));
    } else {
      boolean company = PayerType.fromValue(input.payerType()).get() == PayerType.COMPANY;
      companyField(errors, "companyName", input.companyName(), company, COMPANY_NAME_MAX, CONTROL);
      companyField(
          errors,
          "companyAddress",
          input.companyAddress(),
          company,
          COMPANY_ADDRESS_MAX,
          CONTROL_EXCEPT_LINE_BREAK);
      companyField(errors, "companyVatId", input.companyVatId(), company, VAT_ID_MAX, CONTROL);
    }
    workshops(errors, input.workshops());
    return errors;
  }

  private static void text(
      List<FieldError> errors,
      String field,
      String value,
      boolean required,
      int max,
      Pattern forbidden) {
    if (value == null) {
      if (required) {
        errors.add(new FieldError(field, "is required"));
      }
      return;
    }
    if (value.length() > max) {
      errors.add(new FieldError(field, "must not be longer than " + max + " characters"));
    }
    if (forbidden.matcher(value).find()) {
      errors.add(new FieldError(field, "must not contain control characters"));
    }
  }

  private static void companyField(
      List<FieldError> errors,
      String field,
      String value,
      boolean company,
      int max,
      Pattern forbidden) {
    if (company) {
      text(errors, field, value, true, max, forbidden);
    } else if (value != null) {
      errors.add(new FieldError(field, "must be empty for a private payer"));
    }
  }

  private void workshops(List<FieldError> errors, List<String> workshops) {
    if (workshops.size() > 1) {
      errors.add(new FieldError("workshops", "must contain at most one workshop"));
    } else if (workshops.size() == 1
        && (workshops.get(0) == null || !workshopIds.contains(workshops.get(0)))) {
      errors.add(new FieldError("workshops", "must be a configured workshop id"));
    }
  }
}
