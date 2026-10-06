package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A submitted registration after normalisation: strings trimmed, empty strings treated as absent
 * ({@code null}). The workshop list keeps every submitted entry so that nothing is silently
 * dropped.
 */
public record RegistrationInput(
    String firstName,
    String lastName,
    String email,
    String payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    List<String> workshops) {

  public RegistrationInput {
    firstName = normalize(firstName);
    lastName = normalize(lastName);
    email = normalize(email);
    payerType = normalize(payerType);
    companyName = normalize(companyName);
    companyAddress = normalize(companyAddress);
    companyVatId = normalize(companyVatId);
    workshops = workshops == null ? List.of() : Collections.unmodifiableList(copy(workshops));
  }

  /** Submitted workshop ids, unmodifiable. */
  @Override
  public List<String> workshops() {
    return Collections.unmodifiableList(workshops);
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static List<String> copy(List<String> workshops) {
    List<String> copy = new ArrayList<>(workshops.size());
    for (String workshop : workshops) {
      copy.add(workshop == null ? null : workshop.strip());
    }
    return copy;
  }
}
