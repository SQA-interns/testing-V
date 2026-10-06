package si.confreg.registration.infrastructure.web;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** Counts failed organizer logins per client for the rate limit (SB-06). */
@Component
public class AuthenticationFailureRecorder {

  private final RateLimiter limiter;

  public AuthenticationFailureRecorder(RateLimiter limiter) {
    this.limiter = limiter;
  }

  @EventListener
  public void onFailure(AbstractAuthenticationFailureEvent event) {
    if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails details) {
      limiter.acquire(RateLimiter.Bucket.AUTH_FAILURE, details.getRemoteAddress());
    }
  }
}
