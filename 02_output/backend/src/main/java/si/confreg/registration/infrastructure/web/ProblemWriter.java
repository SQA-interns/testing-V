package si.confreg.registration.infrastructure.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes a fixed-text problem response from a servlet filter (ES-07: no internal details). */
public final class ProblemWriter {

  private ProblemWriter() {}

  public static void write(HttpServletResponse response, int status, String title, String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\""
                + title
                + "\",\"status\":"
                + status
                + ",\"detail\":\""
                + detail
                + "\"}");
  }
}
