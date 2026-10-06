package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The configured workshops, in configured order (D-22, D-28). */
public record WorkshopCatalogue(List<Workshop> workshops) {

  public WorkshopCatalogue {
    workshops = List.copyOf(workshops);
  }

  /** Parses {@code id=title} pairs separated by {@code ;}. */
  public static WorkshopCatalogue parse(String definition) {
    List<Workshop> result = new ArrayList<>();
    for (String pair : definition.split(";")) {
      if (pair.isBlank()) {
        continue;
      }
      String[] parts = pair.split("=", 2);
      if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
        throw new IllegalArgumentException("Workshop definition must be id=title");
      }
      result.add(new Workshop(parts[0].trim(), parts[1].trim()));
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("At least one workshop must be configured");
    }
    return new WorkshopCatalogue(result);
  }

  public Optional<Workshop> find(String id) {
    return workshops.stream().filter(workshop -> workshop.id().equals(id)).findFirst();
  }
}
