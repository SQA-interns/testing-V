package si.confreg.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SR-03: credentials are rejected with 403, before authentication, when they arrive over plain HTTP
 * from a client that is not on the loopback interface. Skipped in the local and test profiles,
 * whose environments listen on 127.0.0.1 only (D-13).
 */
public class InsecureCredentialsFilter extends OncePerRequestFilter {

  /** IPv4 dotted quad, or IPv6 (hex groups, colons, optional embedded IPv4, optional brackets). */
  private static final Pattern IP_LITERAL =
      Pattern.compile("^(\\d{1,3}(\\.\\d{1,3}){3}|\\[?[0-9A-Fa-f:]*:[0-9A-Fa-f:.]*\\]?)$");

  private final boolean plainHttpAllowed;

  public InsecureCredentialsFilter(boolean plainHttpAllowed) {
    this.plainHttpAllowed = plainHttpAllowed;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!plainHttpAllowed
        && request.getHeader(HttpHeaders.AUTHORIZATION) != null
        && !request.isSecure()
        && !isLoopback(request.getRemoteAddr())) {
      Problems.write(response, HttpStatus.FORBIDDEN);
      return;
    }
    chain.doFilter(request, response);
  }

  /** Loopback check on a literal IP address; anything else (a host name) is not loopback. */
  static boolean isLoopback(String address) {
    if (address == null) {
      return false;
    }
    String value = address.strip();
    if (!IP_LITERAL.matcher(value).matches()) {
      return false;
    }
    try {
      // For an IP literal getByName only parses; it never queries a name service.
      return InetAddress.getByName(value).isLoopbackAddress();
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
