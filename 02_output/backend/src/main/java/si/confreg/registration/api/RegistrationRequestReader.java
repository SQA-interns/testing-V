package si.confreg.registration.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import si.confreg.registration.domain.RegistrationInput;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the registration body of the fixed API. Unknown properties and values of the wrong JSON
 * type are reported as invalid fields (AC-001-08, D-23, SB-12); they are never silently dropped.
 */
@Component
class RegistrationRequestReader {

  private static final Set<String> TEXT_FIELDS =
      Set.of(
          "firstName",
          "lastName",
          "email",
          "payerType",
          "companyName",
          "companyAddress",
          "companyVatId");
  private static final String WORKSHOPS = "workshops";

  private final ObjectMapper mapper;

  RegistrationRequestReader(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  record Parsed(RegistrationInput input, List<String> unacceptedFields) {}

  Parsed read(String body) {
    JsonNode root;
    try {
      root = mapper.readTree(body);
    } catch (JacksonException e) {
      throw new MalformedRequestException();
    }
    if (root == null || !root.isObject()) {
      throw new MalformedRequestException();
    }
    List<String> unaccepted = new ArrayList<>();
    for (String name : root.propertyNames()) {
      if (!TEXT_FIELDS.contains(name) && !WORKSHOPS.equals(name)) {
        unaccepted.add(name);
      }
    }
    RegistrationInput input =
        new RegistrationInput(
            text(root, "firstName", unaccepted),
            text(root, "lastName", unaccepted),
            text(root, "email", unaccepted),
            text(root, "payerType", unaccepted),
            text(root, "companyName", unaccepted),
            text(root, "companyAddress", unaccepted),
            text(root, "companyVatId", unaccepted),
            workshops(root, unaccepted));
    return new Parsed(input, unaccepted);
  }

  private static String text(JsonNode root, String field, List<String> unaccepted) {
    JsonNode node = root.get(field);
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isString()) {
      unaccepted.add(field);
      return null;
    }
    return node.stringValue();
  }

  private static List<String> workshops(JsonNode root, List<String> unaccepted) {
    JsonNode node = root.get(WORKSHOPS);
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isArray()) {
      unaccepted.add(WORKSHOPS);
      return null;
    }
    List<String> ids = new ArrayList<>();
    for (JsonNode element : node.values()) {
      if (!element.isString()) {
        unaccepted.add(WORKSHOPS);
        return null;
      }
      ids.add(element.stringValue());
    }
    return ids;
  }
}
