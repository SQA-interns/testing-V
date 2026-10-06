package si.confreg.registration.domain;

import java.util.Objects;

/** A conference workshop from the configuration; workshops have no capacity limit. */
public record Workshop(String id, String title) {

  public Workshop {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
  }
}
