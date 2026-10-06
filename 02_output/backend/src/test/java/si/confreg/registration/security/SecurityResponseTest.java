package si.confreg.registration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.testsupport.TestSettings;

/** Response bodies of the security filters, stream delegation and the limiter's memory bound. */
class SecurityResponseTest {

  private static final Instant T0 = Instant.parse("2030-01-10T10:00:00Z");

  private final ConferenceClock clock = mock(ConferenceClock.class);

  private RateLimiter limiter() {
    when(clock.now()).thenReturn(T0);
    return new RateLimiter(TestSettings.settings(), clock);
  }

  @Test
  void rateLimitFilterPassesAllowedRequestsOnAndAnswersProblemJsonWhenLimited() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(limiter());
    AtomicBoolean passed = new AtomicBoolean();
    for (int i = 0; i < TestSettings.RATE_LIMIT; i++) {
      passed.set(false);
      filter.doFilter(
          new MockHttpServletRequest("POST", "/api/registrations"),
          new MockHttpServletResponse(),
          (req, res) -> passed.set(true));
      assertThat(passed).isTrue();
    }
    MockHttpServletResponse limited = new MockHttpServletResponse();
    passed.set(false);
    filter.doFilter(
        new MockHttpServletRequest("POST", "/api/registrations"),
        limited,
        (req, res) -> passed.set(true));
    assertThat(passed).isFalse();
    assertThat(limited.getContentAsString())
        .isEqualTo("{\"status\":429,\"title\":\"Too Many Requests\"}");
  }

  @Test
  void credentialTransportRejectionIsA401Problem() throws Exception {
    CredentialTransportFilter filter =
        new CredentialTransportFilter(new ProblemAuthenticationEntryPoint(limiter()));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/x");
    request.setServerName("registration.example.org");
    request.addHeader("Authorization", "Basic b3JnOnB3");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString()).isEqualTo(ProblemAuthenticationEntryPoint.BODY);
  }

  @Test
  void bodySizeRejectionIsA413Problem() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[BodySizeFilter.MAX_BODY_BYTES + 1]);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new BodySizeFilter().doFilter(request, response, (req, res) -> {});

    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString())
        .isEqualTo("{\"status\":413,\"title\":\"Payload Too Large\"}");
  }

  private static MockHttpServletRequest undeclaredLength(int bytes) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(new byte[bytes]);
    return request;
  }

  @Test
  void readingByteByByteStopsPastTheLimitAndAllowsTheLimit() throws Exception {
    AtomicReference<Integer> read = new AtomicReference<>(0);
    new BodySizeFilter()
        .doFilter(
            undeclaredLength(BodySizeFilter.MAX_BODY_BYTES),
            new MockHttpServletResponse(),
            (req, res) -> {
              ServletInputStream in = req.getInputStream();
              int count = 0;
              while (in.read() >= 0) {
                count++;
              }
              read.set(count);
            });
    assertThat(read.get()).isEqualTo(BodySizeFilter.MAX_BODY_BYTES);

    assertThatThrownBy(
            () ->
                new BodySizeFilter()
                    .doFilter(
                        undeclaredLength(BodySizeFilter.MAX_BODY_BYTES + 1),
                        new MockHttpServletResponse(),
                        (req, res) -> {
                          ServletInputStream in = req.getInputStream();
                          while (in.read() >= 0) {
                            // drain
                          }
                        }))
        .isInstanceOf(IOException.class);
  }

  @Test
  void limitedStreamDelegatesStateAndListener() throws Exception {
    ServletInputStream delegate = mock(ServletInputStream.class);
    when(delegate.isFinished()).thenReturn(true);
    when(delegate.isReady()).thenReturn(true);
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/x") {
          @Override
          public ServletInputStream getInputStream() {
            return delegate;
          }
        };
    ReadListener listener = mock(ReadListener.class);
    AtomicReference<ServletInputStream> wrapped = new AtomicReference<>();

    new BodySizeFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> wrapped.set(req.getInputStream()));

    assertThat(wrapped.get().isFinished()).isTrue();
    assertThat(wrapped.get().isReady()).isTrue();
    wrapped.get().setReadListener(listener);
    verify(delegate).setReadListener(listener);
    when(delegate.isFinished()).thenReturn(false);
    when(delegate.isReady()).thenReturn(false);
    assertThat(wrapped.get().isFinished()).isFalse();
    assertThat(wrapped.get().isReady()).isFalse();
  }

  @Test
  void limiterEvictsIdleClientsWhenTheMemoryBoundIsReached() {
    RateLimiter limiter = limiter();
    for (int i = 0; i < RateLimiter.MAX_TRACKED; i++) {
      limiter.record("b", "client-" + i);
    }
    assertThat(limiter.tracked()).isEqualTo(RateLimiter.MAX_TRACKED);
    when(clock.now()).thenReturn(T0.plus(Duration.ofHours(2)));

    limiter.tryAcquire("b", "new-client");

    assertThat(limiter.tracked()).isEqualTo(1);
  }

  @Test
  void evictionKeepsActiveClientsAndTheirCounts() {
    RateLimiter limiter = limiter();
    int active = 10;
    for (int i = 0; i < RateLimiter.MAX_TRACKED - active; i++) {
      limiter.record("b", "idle-" + i);
    }
    Instant later = T0.plus(Duration.ofHours(2));
    when(clock.now()).thenReturn(later);
    for (int i = 0; i < active; i++) {
      for (int hit = 0; hit < TestSettings.RATE_LIMIT; hit++) {
        limiter.record("b", "active-" + i);
      }
    }

    limiter.tryAcquire("b", "new-client");

    assertThat(limiter.tracked()).isEqualTo(active + 1);
    assertThat(limiter.exhausted("b", "active-0")).isPresent();
  }

  @Test
  void limiterClearsWhenTheMemoryBoundIsReachedByActiveClients() {
    RateLimiter limiter = limiter();
    for (int i = 0; i < RateLimiter.MAX_TRACKED; i++) {
      limiter.record("b", "client-" + i);
    }

    limiter.tryAcquire("b", "new-client");

    assertThat(limiter.tracked()).isEqualTo(1);
  }

  @Test
  void limiterBelowTheMemoryBoundKeepsAllClients() {
    RateLimiter limiter = limiter();
    for (int i = 0; i < RateLimiter.MAX_TRACKED - 1; i++) {
      limiter.record("b", "client-" + i);
    }

    limiter.tryAcquire("b", "new-client");

    assertThat(limiter.tracked()).isEqualTo(RateLimiter.MAX_TRACKED);
  }
}
