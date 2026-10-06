package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import si.confreg.registration.config.AppSettings;

/**
 * Validates a registration request (AC-001-07, decision D-10). Input is the request body as plain
 * values (strings, numbers, lists, maps, null). Reports at most one error per field.
 */
@Component
public class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int EMAIL_LOCAL_MAX = 64;
  static final int COMPANY_NAME_MAX = 200;
  static final int COMPANY_ADDRESS_MAX = 500;
  static final int VAT_ID_MAX = 30;

  static final String REQUIRED = "is required";

  private static final Pattern CONTROL = Pattern.compile("\\p{Cc}");
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@.]+$");

  private final AppSettings settings;

  public RegistrationValidator(AppSettings settings) {
    this.settings = settings;
  }

  /**
   * Returns the validated data.
   *
   * @throws ValidationFailedException with one error per rejected field
   */
  public RegistrationData validate(Map<String, Object> body) {
    List<FieldError> errors = new ArrayList<>();
    String firstName = requiredText(body, "firstName", NAME_MAX, errors);
    String lastName = requiredText(body, "lastName", NAME_MAX, errors);
    String email = email(body, errors);
    PayerType payerType = payerType(body, errors);
    String companyName = null;
    String companyAddress = null;
    String companyVatId = null;
    if (payerType == PayerType.COMPANY) {
      companyName = requiredText(body, "companyName", COMPANY_NAME_MAX, errors);
      companyAddress = requiredText(body, "companyAddress", COMPANY_ADDRESS_MAX, errors);
      companyVatId = requiredText(body, "companyVatId", VAT_ID_MAX, errors);
    }
    String workshop = workshop(body, errors);
    if (!errors.isEmpty()) {
      throw new ValidationFailedException(errors);
    }
    return new RegistrationData(
        firstName, lastName, email, payerType, companyName, companyAddress, companyVatId, workshop);
  }

  private static String requiredText(
      Map<String, Object> body, String field, int maxLength, List<FieldError> errors) {
    Object value = body.get(field);
    if (value == null) {
      errors.add(new FieldError(field, REQUIRED));
      return null;
    }
    if (!(value instanceof String text)) {
      errors.add(new FieldError(field, "must be text"));
      return null;
    }
    String trimmed = text.strip();
    Optional<String> problem = textProblem(trimmed, maxLength);
    if (problem.isPresent()) {
      errors.add(new FieldError(field, problem.get()));
      return null;
    }
    return trimmed;
  }

  private static Optional<String> textProblem(String trimmed, int maxLength) {
    if (trimmed.isEmpty()) {
      return Optional.of(REQUIRED);
    }
    if (CONTROL.matcher(trimmed).find()) {
      return Optional.of("must not contain control characters");
    }
    if (trimmed.length() > maxLength) {
      return Optional.of("must be at most " + maxLength + " characters");
    }
    return Optional.empty();
  }

  private static String email(Map<String, Object> body, List<FieldError> errors) {
    String email = requiredText(body, "email", EMAIL_MAX, errors);
    if (email == null) {
      return null;
    }
    boolean valid =
        EMAIL.matcher(email).matches()
            && email.indexOf('@') <= EMAIL_LOCAL_MAX
            && !email.contains("..");
    if (!valid) {
      errors.add(new FieldError("email", "must be a valid e-mail address"));
      return null;
    }
    return email;
  }

  private static PayerType payerType(Map<String, Object> body, List<FieldError> errors) {
    Object value = body.get("payerType");
    if (value == null || value instanceof String text && text.isBlank()) {
      errors.add(new FieldError("payerType", REQUIRED));
      return null;
    }
    Optional<PayerType> type =
        value instanceof String text ? PayerType.fromValue(text.strip()) : Optional.empty();
    if (type.isEmpty()) {
      errors.add(new FieldError("payerType", "must be private or company"));
      return null;
    }
    return type.get();
  }

  private String workshop(Map<String, Object> body, List<FieldError> errors) {
    Object value = body.get("workshops");
    if (value == null) {
      return null;
    }
    if (!(value instanceof List<?> list)) {
      errors.add(new FieldError("workshops", "must be a list of workshop ids"));
      return null;
    }
    if (list.isEmpty()) {
      return null;
    }
    if (list.size() > 1) {
      errors.add(new FieldError("workshops", "at most one workshop may be selected"));
      return null;
    }
    Set<String> known = settings.workshopTitles().keySet();
    if (!(list.get(0) instanceof String id) || !known.contains(id)) {
      errors.add(new FieldError("workshops", "unknown workshop"));
      return null;
    }
    return id;
  }
}
