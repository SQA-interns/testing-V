package si.confreg.registration.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/** Writes the documented {@code {"error": code}} body from a filter (SB-07). */
final class JsonError {

  private JsonError() {}

  static void write(HttpServletResponse response, int status, String code) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"error\":\"" + code + "\"}");
  }
}
