package si.confreg.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects credentials sent over plain HTTP to a host other than localhost (SR-03, SB-04). HTTPS is
 * recognised directly or through {@code X-Forwarded-Proto} from a trusted internal proxy.
 */
class CredentialTransportFilter extends OncePerRequestFilter {

  private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  private final ProblemAuthenticationEntryPoint entryPoint;

  CredentialTransportFilter(ProblemAuthenticationEntryPoint entryPoint) {
    this.entryPoint = entryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    boolean hasCredentials = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
    if (hasCredentials && !request.isSecure() && !LOCAL_HOSTS.contains(request.getServerName())) {
      entryPoint.commence(
          request, response, new InsufficientAuthenticationException("Credentials require HTTPS"));
      return;
    }
    chain.doFilter(request, response);
  }
}
