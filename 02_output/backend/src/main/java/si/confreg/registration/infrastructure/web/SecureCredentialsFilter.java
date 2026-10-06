package si.confreg.registration.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses organizer credentials sent over plain HTTP unless the client is local (SR-03, D-16). In
 * profile {@code prod} only loopback counts as local; elsewhere private addresses do too, because
 * Docker forwards the 127.0.0.1-bound port from its bridge gateway.
 */
public class SecureCredentialsFilter extends OncePerRequestFilter {

  private final boolean production;

  public SecureCredentialsFilter(boolean production) {
    this.production = production;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null
        && !request.isSecure()
        && !isLocal(request.getRemoteAddr())) {
      ProblemWriter.write(
          response, 403, "HTTPS required", "HTTPS is required for organizer access.");
      return;
    }
    chain.doFilter(request, response);
  }

  boolean isLocal(String remoteAddress) {
    if (remoteAddress == null || !isIpLiteral(remoteAddress)) {
      return false;
    }
    try {
      InetAddress address = InetAddress.getByName(remoteAddress);
      return address.isLoopbackAddress() || (!production && address.isSiteLocalAddress());
    } catch (UnknownHostException e) {
      return false;
    }
  }

  private static boolean isIpLiteral(String value) {
    return value.chars().allMatch(c -> Character.digit(c, 16) >= 0 || c == '.' || c == ':');
  }
}
