package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raw registration values as submitted, before validation. {@code workshops} is null when absent.
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
    workshops = workshops == null ? null : Collections.unmodifiableList(new ArrayList<>(workshops));
  }
}
