package si.confreg.registration.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import si.confreg.registration.application.RegistrationCommand;

/**
 * Turns the parsed JSON body into a command; a value of the wrong JSON type is reported as an error
 * of that field instead of failing the whole request (specification 4.1).
 */
final class RegistrationRequestMapper {

  private RegistrationRequestMapper() {}

  static RegistrationCommand toCommand(Map<String, Object> body) {
    Set<String> typeErrors = new HashSet<>();
    return new RegistrationCommand(
        text(body, "firstName", typeErrors),
        text(body, "lastName", typeErrors),
        text(body, "email", typeErrors),
        text(body, "payerType", typeErrors),
        text(body, "companyName", typeErrors),
        text(body, "companyAddress", typeErrors),
        text(body, "companyVatId", typeErrors),
        workshops(body, typeErrors),
        bool(body, "student", typeErrors),
        typeErrors);
  }

  private static String text(Map<String, Object> body, String field, Set<String> typeErrors) {
    Object value = body.get(field);
    if (value == null || value instanceof String) {
      return (String) value;
    }
    typeErrors.add(field);
    return null;
  }

  private static Boolean bool(Map<String, Object> body, String field, Set<String> typeErrors) {
    Object value = body.get(field);
    if (value == null || value instanceof Boolean) {
      return (Boolean) value;
    }
    typeErrors.add(field);
    return null;
  }

  private static List<String> workshops(Map<String, Object> body, Set<String> typeErrors) {
    Object value = body.get("workshops");
    if (value == null) {
      return null;
    }
    if (!(value instanceof List<?> list)) {
      typeErrors.add("workshops");
      return null;
    }
    List<String> ids = new ArrayList<>();
    for (Object element : list) {
      if (!(element instanceof String id)) {
        typeErrors.add("workshops");
        return null;
      }
      ids.add(id);
    }
    return ids;
  }
}
