package si.confreg.registration.acceptance.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds registration request bodies with the field names of the fixed API. */
public final class Registrations {

  private Registrations() {}

  /** A unique address per call, so tests never collide on the one-per-e-mail rule. */
  public static String uniqueEmail() {
    return "p-" + UUID.randomUUID() + "@example.org";
  }

  /** A valid private, non-student registration without workshop. */
  public static Map<String, Object> privatePerson() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", uniqueEmail());
    body.put("payerType", "private");
    body.put("workshops", List.of());
    return body;
  }

  /** A valid company-paid, non-student registration without workshop. */
  public static Map<String, Object> company() {
    Map<String, Object> body = privatePerson();
    body.put("payerType", "company");
    body.put("companyName", "Primer d.o.o.");
    body.put("companyAddress", "Slovenska cesta 1\n1000 Ljubljana");
    body.put("companyVatId", "SI12345678");
    return body;
  }

  public static Map<String, Object> with(Map<String, Object> body, String field, Object value) {
    Map<String, Object> copy = new LinkedHashMap<>(body);
    copy.put(field, value);
    return copy;
  }

  public static Map<String, Object> without(Map<String, Object> body, String field) {
    Map<String, Object> copy = new LinkedHashMap<>(body);
    copy.remove(field);
    return copy;
  }
}
