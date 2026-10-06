package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SR-03 (D-27): credentials are accepted only over a secure request, from loopback, or, outside the
 * prod profile, from a private address. Checked before the credentials are read.
 */
class TransportFilter extends OncePerRequestFilter {

  private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F:.]+");

  private final boolean production;

  TransportFilter(boolean production) {
    this.production = production;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null && !acceptable(request)) {
      JsonError.write(response, 403, "insecure_transport");
      return;
    }
    chain.doFilter(request, response);
  }

  private boolean acceptable(HttpServletRequest request) {
    if (request.isSecure()) {
      return true;
    }
    InetAddress client = literalAddress(request.getRemoteAddr());
    if (client == null) {
      return false;
    }
    return client.isLoopbackAddress() || !production && client.isSiteLocalAddress();
  }

  private static InetAddress literalAddress(String address) {
    // only IP literals; never resolve a host name
    if (address == null || !IP_LITERAL.matcher(address).matches()) {
      return null;
    }
    try {
      return InetAddress.getByName(address);
    } catch (UnknownHostException e) {
      return null;
    }
  }
}
