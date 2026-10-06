package si.confreg.registration.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Raw registration input as received, before validation.
 *
 * @param workshops selected workshop ids; {@code null} when absent; elements may be {@code null}
 * @param wrongTypeFields fields whose JSON value had the wrong type (spec 4.2)
 */
public record RegistrationRequest(
    String firstName,
    String lastName,
    String email,
    String payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    List<String> workshops,
    Set<String> wrongTypeFields) {

  public RegistrationRequest {
    workshops = workshops == null ? null : Collections.unmodifiableList(new ArrayList<>(workshops));
    wrongTypeFields = wrongTypeFields == null ? Set.of() : Set.copyOf(wrongTypeFields);
  }
}
