package si.confreg.registration.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits the public endpoints and organizer logins per client (SB-06). The client is the
 * remote address, which the container resolves from the trusted proxy only in profile {@code prod}.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimiter limiter;

  public RateLimitFilter(RateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String client = request.getRemoteAddr();
    long retryAfter = 0;
    String path = request.getRequestURI();
    if ("POST".equals(request.getMethod()) && "/api/registrations".equals(path)) {
      retryAfter = limiter.acquire(RateLimiter.Bucket.REGISTER, client);
    } else if ("GET".equals(request.getMethod()) && "/api/registration-options".equals(path)) {
      retryAfter = limiter.acquire(RateLimiter.Bucket.OPTIONS, client);
    } else if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
      retryAfter = limiter.check(RateLimiter.Bucket.AUTH_FAILURE, client);
    }
    if (retryAfter > 0) {
      response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
      ProblemWriter.write(response, 429, "Too many requests", "Please try again later.");
      return;
    }
    chain.doFilter(request, response);
  }
}
