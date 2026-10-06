package si.confreg.registration.application;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds {@link AppProperties} for unit tests from raw setting values. */
public final class TestSettings {

  private TestSettings() {}

  public static Map<String, String> defaults() {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("conferenceTz", "Europe/Ljubljana");
    values.put("earlyBirdDeadline", "2026-07-31");
    values.put("feeEarly", "240.00");
    values.put("feeRegular", "300.00");
    values.put("vatRate", "0.22");
    values.put("workshops", "W1=One;W2=Two čšž");
    values.put("rateLimitPerHour", "3");
    values.put("testClock", "disabled");
    values.put("mailFrom", "registration@test.example");
    values.put("smtpHost", "localhost");
    values.put("smtpPort", "1025");
    values.put("smtpTls", "false");
    return values;
  }

  public static AppProperties properties() {
    return properties(defaults());
  }

  public static AppProperties with(String name, String value) {
    Map<String, String> values = defaults();
    values.put(name, value);
    return properties(values);
  }

  public static AppProperties properties(Map<String, String> v) {
    return new AppProperties(
        v.get("conferenceTz"),
        v.get("earlyBirdDeadline"),
        v.get("feeEarly"),
        v.get("feeRegular"),
        v.get("vatRate"),
        v.get("workshops"),
        v.get("rateLimitPerHour"),
        v.get("testClock"),
        v.get("mailFrom"),
        v.get("smtpHost"),
        v.get("smtpPort"),
        v.get("smtpTls"));
  }
}
