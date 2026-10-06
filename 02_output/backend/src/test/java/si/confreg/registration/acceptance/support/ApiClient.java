package si.confreg.registration.acceptance.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

/** Calls the registration API over HTTP, as any client would. */
public final class ApiClient {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** An HTTP answer with helpers for the JSON body. */
  public record Response(int status, String body, HttpResponse<byte[]> raw) {

    public Map<String, Object> json() {
      return parse(body);
    }

    public String header(String name) {
      return raw.headers().firstValue(name).orElse(null);
    }

    /** The JSON value of an amount field exactly as written (number or string). */
    public String rawAmount(String field) {
      Matcher matcher =
          Pattern.compile("\"" + field + "\"\\s*:\\s*\"?(-?[0-9]+(?:\\.[0-9]+)?)\"?").matcher(body);
      return matcher.find() ? matcher.group(1) : null;
    }

    public BigDecimal amount(String field) {
      String value = rawAmount(field);
      return value == null ? null : new BigDecimal(value);
    }

    /** Field names listed in the problem's errors array. */
    @SuppressWarnings("unchecked")
    public List<String> errorFields() {
      Object errors = json().get("errors");
      if (!(errors instanceof List<?> list)) {
        return List.of();
      }
      return list.stream()
          .map(e -> String.valueOf(((Map<String, Object>) e).get("field")))
          .toList();
    }
  }

  public Response register(Object requestBody, Instant now) {
    return send(
        HttpRequest.newBuilder(uri("/api/registrations"))
            .header("Content-Type", "application/json")
            .header("X-Test-Now", now.toString())
            .POST(
                HttpRequest.BodyPublishers.ofString(toJson(requestBody), StandardCharsets.UTF_8)));
  }

  public Response options(boolean student, Instant now) {
    return send(
        HttpRequest.newBuilder(uri("/api/registration-options?student=" + student))
            .header("X-Test-Now", now.toString())
            .GET());
  }

  public Response get(String path, String username, String password) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
    if (username != null) {
      String token =
          Base64.getEncoder()
              .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
      builder.header("Authorization", "Basic " + token);
    }
    return send(builder);
  }

  public Response getAsOrganizer(String path) {
    return get(path, AcceptanceConfig.ORGANIZER_USERNAME, AcceptanceConfig.ORGANIZER_PASSWORD);
  }

  public static String toJson(Object value) {
    return value instanceof String s ? s : JSON.writeValueAsString(value);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> parse(String body) {
    return JSON.readValue(body, Map.class);
  }

  private URI uri(String path) {
    return URI.create(baseUrl + path);
  }

  private Response send(HttpRequest.Builder builder) {
    try {
      HttpResponse<byte[]> response =
          http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Response(
          response.statusCode(), new String(response.body(), StandardCharsets.UTF_8), response);
    } catch (IOException e) {
      throw new IllegalStateException("backend not reachable", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
