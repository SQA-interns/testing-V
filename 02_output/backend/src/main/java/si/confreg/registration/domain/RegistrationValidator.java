package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Field rules of AC-001-08 (D-23, SB-01, SR-05, NFR-01). Values are trimmed; empty optional values
 * count as absent.
 */
public final class RegistrationValidator {

  static final int NAME_MAX = 100;
  static final int EMAIL_MAX = 254;
  static final int COMPANY_NAME_MAX = 200;
  static final int COMPANY_ADDRESS_MAX = 500;

  private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M} '’.\\-]*$");
  private static final Pattern EMAIL =
      Pattern.compile(
          "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
              + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
  private static final Pattern VAT_ID = Pattern.compile("^[A-Za-z0-9]{2,20}$");
  private static final Pattern CONTROL_EXCEPT_LINE_BREAKS =
      Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");

  private final WorkshopCatalogue workshops;

  public RegistrationValidator(WorkshopCatalogue workshops) {
    this.workshops = workshops;
  }

  /** Outcome: either the validated participant or the names of the invalid fields. */
  public record Result(Participant participant, List<String> invalidFields) {
    public Result {
      invalidFields = List.copyOf(invalidFields);
    }

    public boolean valid() {
      return invalidFields.isEmpty();
    }
  }

  public Result validate(RegistrationInput input) {
    List<String> invalid = new ArrayList<>();
    String firstName = name(input.firstName(), "firstName", invalid);
    String lastName = name(input.lastName(), "lastName", invalid);
    String email = email(input.email(), invalid);
    Optional<PayerType> payerType = PayerType.fromValue(trimmed(input.payerType()));
    if (payerType.isEmpty()) {
      invalid.add("payerType");
    }
    Company company = company(input, payerType.orElse(null), invalid);
    String workshop = workshop(input.workshops(), invalid);
    if (!invalid.isEmpty()) {
      return new Result(null, List.copyOf(invalid));
    }
    return new Result(
        new Participant(firstName, lastName, email, payerType.get(), company, workshop), List.of());
  }

  private static String trimmed(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String name(String raw, String field, List<String> invalid) {
    String value = trimmed(raw);
    if (value == null || value.length() > NAME_MAX || !NAME.matcher(value).matches()) {
      invalid.add(field);
    }
    return value;
  }

  private static String email(String raw, List<String> invalid) {
    String value = trimmed(raw);
    if (value == null || value.length() > EMAIL_MAX || !EMAIL.matcher(value).matches()) {
      invalid.add("email");
    }
    return value;
  }

  private static Company company(
      RegistrationInput input, PayerType payerType, List<String> invalid) {
    String name = trimmed(input.companyName());
    String address = trimmed(input.companyAddress());
    String vatId = trimmed(input.companyVatId());
    if (payerType == PayerType.PRIVATE) {
      requireAbsent(name, "companyName", invalid);
      requireAbsent(address, "companyAddress", invalid);
      requireAbsent(vatId, "companyVatId", invalid);
      return null;
    }
    if (payerType == PayerType.COMPANY) {
      if (!text(name, COMPANY_NAME_MAX)) {
        invalid.add("companyName");
      }
      if (!text(address, COMPANY_ADDRESS_MAX)) {
        invalid.add("companyAddress");
      }
      if (vatId == null || !VAT_ID.matcher(vatId).matches()) {
        invalid.add("companyVatId");
      }
      return new Company(name, address, vatId);
    }
    return null;
  }

  private static void requireAbsent(String value, String field, List<String> invalid) {
    if (value != null) {
      invalid.add(field);
    }
  }

  private static boolean text(String value, int max) {
    return value != null
        && value.length() <= max
        && !CONTROL_EXCEPT_LINE_BREAKS.matcher(value).find();
  }

  private String workshop(List<String> requested, List<String> invalid) {
    if (requested == null || requested.isEmpty()) {
      return null;
    }
    if (requested.size() > 1) {
      invalid.add("workshops");
      return null;
    }
    String id = trimmed(requested.get(0));
    if (id == null || workshops.find(id).isEmpty()) {
      invalid.add("workshops");
      return null;
    }
    return id;
  }
}
