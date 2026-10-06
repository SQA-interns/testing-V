package si.confreg.registration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.testsupport.TestSettings;

/** Rate limiting (SB-06, D-11), body size (SR-02), credential transport (SR-03), 401 body. */
class SecurityUnitTest {

  private static final Instant T0 = Instant.parse("2030-01-10T10:00:00Z");
  private static final int LIMIT = TestSettings.RATE_LIMIT;

  private final ConferenceClock clock = mock(ConferenceClock.class);
  private RateLimiter limiter;

  @BeforeEach
  void setUp() {
    when(clock.now()).thenReturn(T0);
    limiter = new RateLimiter(TestSettings.settings(), clock);
  }

  @Test
  void limiterAllowsUpToTheLimitThenAnswersRetryAfter() {
    for (int i = 0; i < LIMIT; i++) {
      assertThat(limiter.tryAcquire("b", "1.2.3.4")).isEmpty();
    }
    OptionalLong retry = limiter.tryAcquire("b", "1.2.3.4");
    assertThat(retry).hasValue(Duration.ofHours(1).toSeconds());
  }

  @Test
  void limiterCountsClientsAndBucketsSeparately() {
    for (int i = 0; i < LIMIT; i++) {
      limiter.tryAcquire("b", "1.2.3.4");
    }
    assertThat(limiter.tryAcquire("b", "5.6.7.8")).isEmpty();
    assertThat(limiter.tryAcquire("other", "1.2.3.4")).isEmpty();
  }

  @Test
  void limiterFreesTheOldestRequestAfterOneHour() {
    for (int i = 0; i < LIMIT; i++) {
      limiter.tryAcquire("b", "c");
    }
    when(clock.now()).thenReturn(T0.plus(Duration.ofMinutes(59)));
    assertThat(limiter.tryAcquire("b", "c")).hasValue(60);
    when(clock.now()).thenReturn(T0.plus(Duration.ofHours(1)));
    assertThat(limiter.tryAcquire("b", "c")).isEmpty();
  }

  @Test
  void recordedFailuresExhaustTheBudget() {
    assertThat(limiter.exhausted("f", "c")).isEmpty();
    for (int i = 0; i < LIMIT - 1; i++) {
      limiter.record("f", "c");
    }
    assertThat(limiter.exhausted("f", "c")).isEmpty();
    limiter.record("f", "c");
    assertThat(limiter.exhausted("f", "c")).isPresent();
    when(clock.now()).thenReturn(T0.plus(Duration.ofHours(2)));
    assertThat(limiter.exhausted("f", "c")).isEmpty();
  }

  private MockHttpServletResponse filter(RateLimitFilter filter, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, (req, res) -> {});
    return response;
  }

  @Test
  void rateLimitFilterRejectsRegistrationOverLimitWith429AndRetryAfter() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(limiter);
    for (int i = 0; i < LIMIT; i++) {
      assertThat(
              filter(filter, new MockHttpServletRequest("POST", "/api/registrations")).getStatus())
          .isEqualTo(200);
    }
    MockHttpServletResponse response =
        filter(filter, new MockHttpServletRequest("POST", "/api/registrations"));
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("3600");
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
  }

  @Test
  void rateLimitFilterBlocksOrganizerPathsAfterFailedAuthentications() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(limiter);
    MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/registrations/CR-1");
    assertThat(filter(filter, get).getStatus()).isEqualTo(200);
    for (int i = 0; i < LIMIT; i++) {
      limiter.record(RateLimitFilter.FAILED_AUTHENTICATION, get.getRemoteAddr());
    }
    assertThat(filter(filter, get).getStatus()).isEqualTo(429);
    // registrations use their own budget
    assertThat(filter(filter, new MockHttpServletRequest("POST", "/api/registrations")).getStatus())
        .isEqualTo(200);
  }

  @Test
  void rateLimitFilterIgnoresNonApiPaths() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(limiter);
    for (int i = 0; i < LIMIT; i++) {
      limiter.record(RateLimitFilter.FAILED_AUTHENTICATION, "127.0.0.1");
    }
    assertThat(filter(filter, new MockHttpServletRequest("GET", "/actuator/health")).getStatus())
        .isEqualTo(200);
  }

  @Test
  void entryPointAnswers401ProblemWithChallengeAndCountsTheFailure() throws Exception {
    ProblemAuthenticationEntryPoint entryPoint = new ProblemAuthenticationEntryPoint(limiter);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/x");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("bad"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).startsWith("Basic");
    assertThat(response.getContentAsString()).isEqualTo(ProblemAuthenticationEntryPoint.BODY);
    for (int i = 1; i < LIMIT; i++) {
      limiter.record(RateLimitFilter.FAILED_AUTHENTICATION, request.getRemoteAddr());
    }
    assertThat(limiter.exhausted(RateLimitFilter.FAILED_AUTHENTICATION, request.getRemoteAddr()))
        .isPresent();
  }

  private static boolean passes(CredentialTransportFilter filter, MockHttpServletRequest request)
      throws Exception {
    AtomicBoolean passed = new AtomicBoolean();
    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.set(true));
    return passed.get();
  }

  private static MockHttpServletRequest withCredentials(String host, boolean secure) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/x");
    request.setServerName(host);
    request.setSecure(secure);
    request.addHeader("Authorization", "Basic b3JnOnB3");
    return request;
  }

  @Test
  void credentialsArePassedOnLocalhostOrHttpsOnly() throws Exception {
    CredentialTransportFilter filter =
        new CredentialTransportFilter(new ProblemAuthenticationEntryPoint(limiter));
    assertThat(passes(filter, withCredentials("localhost", false))).isTrue();
    assertThat(passes(filter, withCredentials("127.0.0.1", false))).isTrue();
    assertThat(passes(filter, withCredentials("registration.example.org", true))).isTrue();
    assertThat(passes(filter, withCredentials("registration.example.org", false))).isFalse();
    MockHttpServletRequest withoutCredentials = new MockHttpServletRequest("GET", "/x");
    withoutCredentials.setServerName("registration.example.org");
    assertThat(passes(filter, withoutCredentials)).isTrue();
  }

  @Test
  void bodySizeFilterRejectsDeclaredLargeBodyWith413() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[BodySizeFilter.MAX_BODY_BYTES + 1]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean called = new AtomicBoolean();

    new BodySizeFilter().doFilter(request, response, (req, res) -> called.set(true));

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(called).isFalse();
  }

  @Test
  void bodySizeFilterAllowsBodyAtTheLimit() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[BodySizeFilter.MAX_BODY_BYTES]);
    AtomicReference<Integer> read = new AtomicReference<>();
    FilterChain chain = (req, res) -> read.set(req.getInputStream().readAllBytes().length);

    new BodySizeFilter().doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(read.get()).isEqualTo(BodySizeFilter.MAX_BODY_BYTES);
  }

  @Test
  void bodySizeFilterStopsReadingAnUndeclaredLargeBody() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(new byte[BodySizeFilter.MAX_BODY_BYTES + 10]);
    FilterChain chain = (req, res) -> req.getInputStream().readAllBytes();

    assertThatThrownBy(
            () -> new BodySizeFilter().doFilter(request, new MockHttpServletResponse(), chain))
        .isInstanceOf(IOException.class);
  }
}
