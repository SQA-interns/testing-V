package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses credentials sent over plain HTTP unless the client is on the loopback interface or the
 * local stack explicitly allows it (SR-03, D-26). Runs before authentication.
 */
public class InsecureCredentialsFilter extends OncePerRequestFilter {

  private final boolean insecureAllowed;

  public InsecureCredentialsFilter(boolean insecureAllowed) {
    this.insecureAllowed = insecureAllowed;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null
        && !insecureAllowed
        && !request.isSecure()
        && !isLoopback(request.getRemoteAddr())) {
      ProblemResponses.write(
          response, HttpStatus.FORBIDDEN, "Credentials are accepted only over HTTPS.");
      return;
    }
    chain.doFilter(request, response);
  }

  static boolean isLoopback(String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    if (!address.matches("[0-9.]+|[0-9a-fA-F.]*:[0-9a-fA-F:.]*")) {
      return false; // a literal IP address only; never resolve host names
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
