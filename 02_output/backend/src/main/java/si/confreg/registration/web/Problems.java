package si.confreg.registration.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * RFC 9457 problem responses with fixed titles only: no exception messages, stack traces or input
 * values (SB-07, ES-07).
 */
public final class Problems {

  static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

  private Problems() {}

  static Map<String, Object> body(HttpStatus status) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", status.getReasonPhrase());
    body.put("status", status.value());
    return body;
  }

  static ResponseEntity<Object> response(HttpStatus status) {
    return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(body(status));
  }

  /** Writes a problem directly to the servlet response (for filters and security handlers). */
  public static void write(HttpServletResponse response, HttpStatus status) throws IOException {
    response.setStatus(status.value());
    response.setContentType(PROBLEM_JSON.toString());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\""
                + status.getReasonPhrase()
                + "\",\"status\":"
                + status.value()
                + "}");
  }
}
