package si.confreg.registration.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Configured workshops in configured order. */
public final class WorkshopCatalog {

  private final Map<String, Workshop> byId;

  public WorkshopCatalog(List<Workshop> workshops) {
    Map<String, Workshop> map = new LinkedHashMap<>();
    for (Workshop workshop : workshops) {
      if (map.putIfAbsent(workshop.id(), workshop) != null) {
        throw new IllegalArgumentException("duplicate workshop id " + workshop.id());
      }
    }
    this.byId = map;
  }

  /**
   * Parses {@code id=title;id=title} (D-14). Blank entries are ignored.
   *
   * @throws IllegalArgumentException when an entry has no {@code =}, or an empty id or title
   */
  public static WorkshopCatalog parse(String encoded) {
    List<Workshop> workshops = new ArrayList<>();
    if (encoded != null) {
      for (String entry : encoded.split(";")) {
        if (entry.isBlank()) {
          continue;
        }
        int separator = entry.indexOf('=');
        if (separator < 0) {
          throw new IllegalArgumentException("workshop entry without '=': " + entry.strip());
        }
        String id = entry.substring(0, separator).strip();
        String title = entry.substring(separator + 1).strip();
        if (id.isEmpty() || title.isEmpty()) {
          throw new IllegalArgumentException("workshop entry with empty id or title");
        }
        workshops.add(new Workshop(id, title));
      }
    }
    return new WorkshopCatalog(workshops);
  }

  public List<Workshop> all() {
    return List.copyOf(byId.values());
  }

  public Optional<Workshop> find(String id) {
    return Optional.ofNullable(byId.get(id));
  }

  public boolean contains(String id) {
    return byId.containsKey(id);
  }
}
