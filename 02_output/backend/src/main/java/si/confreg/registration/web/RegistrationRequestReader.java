package si.confreg.registration.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import si.confreg.registration.application.RegistrationRequest;
import tools.jackson.databind.JsonNode;

/**
 * Maps the JSON body to a {@link RegistrationRequest}. A value of the wrong JSON type for a known
 * field is reported for that field (spec 4.2); unknown properties are ignored.
 */
final class RegistrationRequestReader {

  private RegistrationRequestReader() {}

  static RegistrationRequest read(JsonNode body) {
    Set<String> wrongType = new HashSet<>();
    return new RegistrationRequest(
        text(body, "firstName", wrongType),
        text(body, "lastName", wrongType),
        text(body, "email", wrongType),
        text(body, "payerType", wrongType),
        text(body, "companyName", wrongType),
        text(body, "companyAddress", wrongType),
        text(body, "companyVatId", wrongType),
        workshops(body, wrongType),
        wrongType);
  }

  private static String text(JsonNode body, String field, Set<String> wrongType) {
    JsonNode node = body.get(field);
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isString()) {
      return node.asString();
    }
    wrongType.add(field);
    return null;
  }

  private static List<String> workshops(JsonNode body, Set<String> wrongType) {
    JsonNode node = body.get("workshops");
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isArray()) {
      wrongType.add("workshops");
      return null;
    }
    List<String> ids = new ArrayList<>();
    for (JsonNode element : node) {
      if (element.isString()) {
        ids.add(element.asString());
      } else if (element.isNull()) {
        ids.add(null);
      } else {
        wrongType.add("workshops");
        return null;
      }
    }
    return ids;
  }
}
