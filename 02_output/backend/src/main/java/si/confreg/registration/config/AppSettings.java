package si.confreg.registration.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed application settings (prefix {@code app}), each overridable by the environment variable of
 * the same name (environments.md, AR-04). Validated at startup.
 */
@Validated
@ConfigurationProperties("app")
public record AppSettings(
    @NotNull ZoneId conferenceTz,
    @NotNull LocalDate earlyBirdDeadline,
    @NotNull @PositiveOrZero BigDecimal feeEarly,
    @NotNull @PositiveOrZero BigDecimal feeRegular,
    @NotNull @PositiveOrZero BigDecimal vatRate,
    @NotBlank String workshops,
    @Positive int rateLimitPerHour,
    @NotNull TestClockMode testClock,
    @NotBlank String mailFrom,
    @NotNull Duration mailRetryInterval,
    @Positive int mailMaxAttempts,
    @Valid @NotNull Organizer organizer) {

  /** Whether the request header {@code X-Test-Now} may set the current time. */
  public enum TestClockMode {
    ENABLED,
    DISABLED
  }

  /** Organizer credentials; no defaults (ES-01). */
  public record Organizer(@NotBlank String username, @NotBlank String password) {

    @Override
    public String toString() {
      return "Organizer[username=" + username + ", password=****]";
    }
  }

  public boolean testClockEnabled() {
    return testClock == TestClockMode.ENABLED;
  }

  /**
   * Configured workshops in order, id to title, parsed from {@code id=title;id=title}.
   *
   * @throws IllegalStateException if an entry has no id or an id repeats
   */
  public Map<String, String> workshopTitles() {
    Map<String, String> result = new LinkedHashMap<>();
    for (String entry : workshops.split(";")) {
      if (entry.isBlank()) {
        continue;
      }
      String[] parts = entry.split("=", 2);
      String id = parts[0].trim();
      String title = parts.length > 1 ? parts[1].trim() : id;
      if (id.isEmpty() || result.putIfAbsent(id, title) != null) {
        throw new IllegalStateException("app.workshops has an empty or repeated workshop id");
      }
    }
    return Collections.unmodifiableMap(result);
  }
}
