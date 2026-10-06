package si.confreg.registration.application;

import java.util.List;
import java.util.Set;

/**
 * A registration request as received. Strings are untrimmed; a field listed in {@code typeErrors}
 * had a JSON value of the wrong type and is {@code null} here.
 */
public record RegistrationCommand(
    String firstName,
    String lastName,
    String email,
    String payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    List<String> workshops,
    boolean student,
    Set<String> typeErrors) {

  public RegistrationCommand {
    workshops = workshops == null ? null : List.copyOf(workshops);
    typeErrors = typeErrors == null ? Set.of() : Set.copyOf(typeErrors);
  }
}
