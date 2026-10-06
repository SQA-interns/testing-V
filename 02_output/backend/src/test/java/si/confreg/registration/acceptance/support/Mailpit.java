package si.confreg.registration.acceptance.support;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Reads what reached the mail catcher through its HTTP API. */
public final class Mailpit {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  public Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** A received message: subject, plain-text body, HTML body and Content-Type header. */
  public record Message(String subject, String text, String html, String contentType) {}

  @SuppressWarnings("unchecked")
  public List<String> idsTo(String address) {
    String query = URLEncoder.encode("to:\"" + address + "\"", StandardCharsets.UTF_8);
    Map<String, Object> result = getJson("/api/v1/search?query=" + query);
    List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");
    return messages == null ? List.of() : messages.stream().map(m -> (String) m.get("ID")).toList();
  }

  @SuppressWarnings("unchecked")
  public Message message(String id) {
    Map<String, Object> message = getJson("/api/v1/message/" + id);
    Map<String, Object> headers = getJson("/api/v1/message/" + id + "/headers");
    List<String> contentType = (List<String>) headers.get("Content-Type");
    return new Message(
        (String) message.get("Subject"),
        (String) message.get("Text"),
        (String) message.get("HTML"),
        contentType == null || contentType.isEmpty() ? "" : contentType.get(0));
  }

  /** Waits until at least one message to the address arrived and returns the first. */
  public Message awaitMessageTo(String address, Duration timeout) {
    await()
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(500))
        .until(() -> !idsTo(address).isEmpty());
    return message(idsTo(address).get(0));
  }

  /** Number of messages to the address after a short settling time. */
  public int countTo(String address, Duration settle) {
    try {
      Thread.sleep(settle.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return idsTo(address).size();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getJson(String path) {
    try {
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
              HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return JSON.readValue(response.body(), Map.class);
    } catch (IOException e) {
      throw new IllegalStateException("Mailpit not reachable", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
