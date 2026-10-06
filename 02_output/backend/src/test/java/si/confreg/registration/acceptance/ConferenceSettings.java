package si.confreg.registration.acceptance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Business settings for the acceptance tests, read from the configuration table of {@code
 * 01_input/01_project/00_setup/environments.md} (REQ-REG-01 "Data and fixtures": values come from
 * that configuration, never from literals). The tests start the backend with exactly these settings
 * and compute every expected value from them.
 */
final class ConferenceSettings {

  static final Path ENVIRONMENTS = Path.of("01_input", "01_project", "00_setup", "environments.md");

  private static final Pattern ROW =
      Pattern.compile("^\\|\\s*`(APP_[A-Z_]+)`\\s*\\|\\s*(.*?)\\s*\\|");
  private static final Pattern WORKSHOP = Pattern.compile("`([^`]+)`\\s*([^;]+)");

  private final Map<String, String> values;

  private ConferenceSettings(Map<String, String> values) {
    this.values = values;
  }

  static ConferenceSettings load() {
    Path file = locate();
    Map<String, String> values = new LinkedHashMap<>();
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        Matcher row = ROW.matcher(line.strip());
        if (row.find()) {
          values.put(row.group(1), row.group(2).strip());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new ConferenceSettings(values);
  }

  private static Path locate() {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null) {
      Path candidate = dir.resolve(ENVIRONMENTS);
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException(
        "environments.md not found above " + Path.of("").toAbsolutePath());
  }

  String get(String name) {
    String value = values.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("setting " + name + " missing in " + ENVIRONMENTS);
    }
    return value;
  }

  /** Workshops in configured order: id to title. */
  Map<String, String> workshops() {
    Map<String, String> result = new LinkedHashMap<>();
    Matcher matcher = WORKSHOP.matcher(get("APP_WORKSHOPS"));
    while (matcher.find()) {
      result.put(matcher.group(1).strip(), matcher.group(2).strip());
    }
    if (result.isEmpty()) {
      throw new IllegalStateException("no workshops in APP_WORKSHOPS");
    }
    return result;
  }

  /** {@code APP_WORKSHOPS} in the backend's encoding {@code id=title;id=title} (D-14). */
  String workshopsProperty() {
    StringBuilder encoded = new StringBuilder();
    workshops()
        .forEach(
            (id, title) -> {
              if (!encoded.isEmpty()) {
                encoded.append(';');
              }
              encoded.append(id).append('=').append(title);
            });
    return encoded.toString();
  }
}
