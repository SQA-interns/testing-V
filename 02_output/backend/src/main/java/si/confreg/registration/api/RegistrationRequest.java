package si.confreg.registration.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import si.confreg.registration.domain.RegistrationInput;

/** Body of {@code POST /api/registrations} (fixed registration API). */
public record RegistrationRequest(
    String firstName,
    String lastName,
    String email,
    String payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    List<String> workshops) {

  public RegistrationRequest {
    workshops = workshops == null ? null : Collections.unmodifiableList(new ArrayList<>(workshops));
  }

  /** Submitted workshop ids, unmodifiable; {@code null} when absent. */
  @Override
  public List<String> workshops() {
    return workshops == null ? null : Collections.unmodifiableList(workshops);
  }

  RegistrationInput toInput() {
    return new RegistrationInput(
        firstName,
        lastName,
        email,
        payerType,
        companyName,
        companyAddress,
        companyVatId,
        workshops);
  }
}
