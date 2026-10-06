package si.confreg.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import si.confreg.registration.application.RateLimiter;

/**
 * Per-client rate limits (spec 7.4, SB-06, AC-001-12): the public registration and workshop
 * endpoints have one bucket each; failed organizer logins have a third bucket that, once used up,
 * blocks further requests with credentials. The client is the remote address after the trusted
 * proxy's forwarded headers were applied.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  static final String REGISTRATIONS = "/api/registrations";
  static final String WORKSHOPS = "/api/workshops";

  private final RateLimiter limiter;

  public RateLimitFilter(RateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String client = request.getRemoteAddr();
    String path = request.getRequestURI().substring(request.getContextPath().length());
    String bucket = publicBucket(request.getMethod(), path);
    boolean withCredentials = request.getHeader(HttpHeaders.AUTHORIZATION) != null;

    if (withCredentials && reject(limiter.check("auth-failure|" + client), response)) {
      return;
    }
    if (bucket != null && reject(limiter.tryAcquire(bucket + "|" + client), response)) {
      return;
    }
    chain.doFilter(request, response);
    if (withCredentials && response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
      limiter.record("auth-failure|" + client);
    }
  }

  private static String publicBucket(String method, String path) {
    if (HttpMethod.POST.matches(method) && REGISTRATIONS.equals(path)) {
      return "registration";
    }
    if (HttpMethod.GET.matches(method) && WORKSHOPS.equals(path)) {
      return "workshops";
    }
    return null;
  }

  private static boolean reject(RateLimiter.Decision decision, HttpServletResponse response)
      throws IOException {
    if (decision.allowed()) {
      return false;
    }
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
    Problems.write(response, HttpStatus.TOO_MANY_REQUESTS);
    return true;
  }
}
