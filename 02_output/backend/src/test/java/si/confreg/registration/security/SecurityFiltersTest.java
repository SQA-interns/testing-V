package si.confreg.registration.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityFiltersTest {

  // ---- rate limit (SB-06) ----

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static MockHttpServletResponse call(RateLimitFilter filter, String client, String uri)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
    request.setRemoteAddr(client);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(201));
    return response;
  }

  @Test
  void rateLimitPerClientAndWindow() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2030-01-01T10:59:00Z"));
    RateLimitFilter filter = new RateLimitFilter(2, clock);

    assertThat(call(filter, "10.0.0.1", "/api/registrations").getStatus()).isEqualTo(201);
    assertThat(call(filter, "10.0.0.1", "/api/registrations/REG-1").getStatus()).isEqualTo(201);
    MockHttpServletResponse limited = call(filter, "10.0.0.1", "/api/registrations");
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    assertThat(limited.getContentType()).startsWith("application/problem+json");
    assertThat(call(filter, "10.0.0.2", "/api/registrations").getStatus()).isEqualTo(201);

    clock.now = clock.now.plus(Duration.ofMinutes(1));
    assertThat(call(filter, "10.0.0.1", "/api/registrations").getStatus()).isEqualTo(201);
  }

  @Test
  void rateLimitAppliesOnlyToApi() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(1, new MutableClock(Instant.EPOCH));

    for (int i = 0; i < 3; i++) {
      assertThat(call(filter, "10.0.0.1", "/actuator/health").getStatus()).isEqualTo(201);
    }
  }

  @Test
  void rateLimitForgetsOldWindowsWhenManyClients() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2030-01-01T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    for (int i = 0; i <= 10_001; i++) {
      call(filter, "client-" + i, "/api/registrations");
    }
    clock.now = clock.now.plus(Duration.ofHours(1));

    assertThat(call(filter, "client-0", "/api/registrations").getStatus()).isEqualTo(201);
    assertThat(call(filter, "client-0", "/api/registrations").getStatus()).isEqualTo(429);
  }

  // ---- body size (SR-02) ----

  @Test
  void bodyAtLimitIsPassedOnUnchanged() throws Exception {
    byte[] body = "x".repeat(BodySizeLimitFilter.MAX_BODY_BYTES).getBytes(StandardCharsets.UTF_8);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(body);
    request.setCharacterEncoding("UTF-8");
    AtomicReference<String> seen = new AtomicReference<>();
    AtomicInteger length = new AtomicInteger();
    FilterChain chain =
        (req, res) -> {
          length.set(req.getContentLength());
          seen.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        };

    new BodySizeLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(seen.get()).hasSize(BodySizeLimitFilter.MAX_BODY_BYTES);
    assertThat(length.get()).isEqualTo(BodySizeLimitFilter.MAX_BODY_BYTES);
  }

  @Test
  void readerSeesBufferedBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent("{\"a\":\"č\"}".getBytes(StandardCharsets.UTF_8));
    AtomicReference<String> seen = new AtomicReference<>();

    new BodySizeLimitFilter()
        .doFilter(
            request,
            new MockHttpServletResponse(),
            (req, res) -> seen.set(req.getReader().readLine()));

    assertThat(seen.get()).isEqualTo("{\"a\":\"č\"}");
  }

  @Test
  void bodyOverLimitIsRefusedWhileReading() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[BodySizeLimitFilter.MAX_BODY_BYTES + 1]);
    request.removeHeader("Content-Length");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Boolean> called = new AtomicReference<>(false);

    new BodySizeLimitFilter().doFilter(request, response, (req, res) -> called.set(true));

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(called.get()).isFalse();
  }

  @Test
  void declaredLengthOverLimitIsRefusedBeforeReading() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return BodySizeLimitFilter.MAX_BODY_BYTES + 1L;
          }
        };
    MockHttpServletResponse response = new MockHttpServletResponse();

    new BodySizeLimitFilter().doFilter(request, response, (req, res) -> res.getWriter().write("x"));

    assertThat(response.getStatus()).isEqualTo(413);
  }

  @Test
  void getRequestsAreNotBuffered() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/X");
    AtomicReference<Object> seen = new AtomicReference<>();

    new BodySizeLimitFilter()
        .doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(req));

    assertThat(seen.get()).isSameAs(request);
  }

  // ---- credentials over plain HTTP (SR-03) ----

  private static MockHttpServletResponse withCredentials(
      boolean allowed, String remote, boolean secure) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/X");
    request.addHeader("Authorization", "Basic eDp5");
    request.setRemoteAddr(remote);
    request.setSecure(secure);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new InsecureCredentialsFilter(allowed)
        .doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(200));
    return response;
  }

  @Test
  void credentialsOverPlainHttpFromRemoteClientRefused() throws Exception {
    assertThat(withCredentials(false, "203.0.113.5", false).getStatus()).isEqualTo(403);
  }

  @Test
  void credentialsAcceptedOverHttpsLoopbackOrWhenAllowed() throws Exception {
    assertThat(withCredentials(false, "203.0.113.5", true).getStatus()).isEqualTo(200);
    assertThat(withCredentials(false, "127.0.0.1", false).getStatus()).isEqualTo(200);
    assertThat(withCredentials(false, "::1", false).getStatus()).isEqualTo(200);
    assertThat(withCredentials(true, "203.0.113.5", false).getStatus()).isEqualTo(200);
  }

  @Test
  void requestsWithoutCredentialsPass() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setRemoteAddr("203.0.113.5");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new InsecureCredentialsFilter(false)
        .doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(201));

    assertThat(response.getStatus()).isEqualTo(201);
  }

  @ParameterizedTest
  @ValueSource(strings = {"localhost", "cafe", "", "10.0.0.1", "0:0:0:0:0:0:0:2", "example.com"})
  void onlyLoopbackLiteralsAreLoopback(String address) {
    assertThat(InsecureCredentialsFilter.isLoopback(address)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.1.2.3", "::1", "0:0:0:0:0:0:0:1"})
  void loopbackLiteralsAreLoopback(String address) {
    assertThat(InsecureCredentialsFilter.isLoopback(address)).isTrue();
  }

  @Test
  void nullAddressIsNotLoopback() {
    assertThat(InsecureCredentialsFilter.isLoopback(null)).isFalse();
  }
}
