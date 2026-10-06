package si.confreg.registration.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Writes RFC 9457 problem responses from filters, with fixed texts only (SB-07). */
final class ProblemResponses {

  private ProblemResponses() {}

  static void write(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"title\":\""
                + status.getReasonPhrase()
                + "\",\"status\":"
                + status.value()
                + ",\"detail\":\""
                + detail
                + "\"}");
  }
}
