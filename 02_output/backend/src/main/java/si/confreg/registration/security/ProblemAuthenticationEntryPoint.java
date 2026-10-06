package si.confreg.registration.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Answers 401 as problem details with a Basic challenge and no other data (AC-001-11). */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

  static final String BODY = "{\"status\":401,\"title\":\"Unauthorized\"}";

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"organizer\"");
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(BODY);
  }
}
