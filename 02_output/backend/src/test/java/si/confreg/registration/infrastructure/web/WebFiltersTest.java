package si.confreg.registration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.application.PayloadTooLargeException;
import si.confreg.registration.application.TestSettings;

class WebFiltersTest {

  private static MockHttpServletRequest request(String method, String uri, String remote) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(remote);
    return request;
  }

  // --- rate limit (SB-06) ---

  @Test
  void rateLimitAnswers429WithRetryAfterOnTheFourthRegistration() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(
            new RateLimiter(
                TestSettings.properties(), () -> Instant.parse("2026-08-01T10:00:00Z")));
    MockHttpServletResponse response = null;
    for (int i = 0; i < 4; i++) {
      response = new MockHttpServletResponse();
      filter.doFilter(
          request("POST", "/api/registrations", "1.2.3.4"), response, new MockFilterChain());
    }

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("3600");
    assertThat(response.getContentType()).startsWith("application/problem+json");
  }

  @Test
  void rateLimitIgnoresHealthAndOrganizerRequestsWithoutFailures() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(new RateLimiter(TestSettings.properties(), Instant::now));
    for (int i = 0; i < 10; i++) {
      MockHttpServletResponse health = new MockHttpServletResponse();
      filter.doFilter(request("GET", "/actuator/health", "1.2.3.4"), health, new MockFilterChain());
      assertThat(health.getStatus()).isEqualTo(200);
      MockHttpServletRequest organizer = request("GET", "/api/registrations/export", "1.2.3.4");
      organizer.addHeader("Authorization", "Basic eDp5");
      MockHttpServletResponse ok = new MockHttpServletResponse();
      filter.doFilter(organizer, ok, new MockFilterChain());
      assertThat(ok.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void rateLimitBlocksCredentialsAfterTooManyFailures() throws Exception {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-01T10:00:00Z"));
    RateLimiter limiter = new RateLimiter(TestSettings.properties(), now::get);
    for (int i = 0; i < 3; i++) {
      limiter.acquire(RateLimiter.Bucket.AUTH_FAILURE, "1.2.3.4");
    }
    MockHttpServletRequest organizer = request("GET", "/api/registrations/CR-000001", "1.2.3.4");
    organizer.addHeader("Authorization", "Basic eDp5");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RateLimitFilter(limiter).doFilter(organizer, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(429);
  }

  @Test
  void optionsHaveTheirOwnBucket() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(new RateLimiter(TestSettings.properties(), Instant::now));
    for (int i = 0; i < 3; i++) {
      filter.doFilter(
          request("GET", "/api/registration-options", "1.2.3.4"),
          new MockHttpServletResponse(),
          new MockFilterChain());
    }
    MockHttpServletResponse options = new MockHttpServletResponse();
    filter.doFilter(
        request("GET", "/api/registration-options", "1.2.3.4"), options, new MockFilterChain());
    MockHttpServletResponse register = new MockHttpServletResponse();
    filter.doFilter(
        request("POST", "/api/registrations", "1.2.3.4"), register, new MockFilterChain());

    assertThat(options.getStatus()).isEqualTo(429);
    assertThat(register.getStatus()).isEqualTo(200);
  }

  @Test
  void allowedRequestsArePassedOn() throws Exception {
    MockFilterChain rateChain = new MockFilterChain();
    new RateLimitFilter(new RateLimiter(TestSettings.properties(), Instant::now))
        .doFilter(
            request("POST", "/api/registrations", "1.2.3.4"),
            new MockHttpServletResponse(),
            rateChain);
    MockFilterChain credentialsChain = new MockFilterChain();
    new SecureCredentialsFilter(true)
        .doFilter(
            request("GET", "/api/registrations/export", "127.0.0.1"),
            new MockHttpServletResponse(),
            credentialsChain);

    assertThat(rateChain.getRequest()).isNotNull();
    assertThat(credentialsChain.getRequest()).isNotNull();
  }

  @Test
  void refusedRequestsAreNotPassedOn() throws Exception {
    MockHttpServletRequest organizer = request("GET", "/api/registrations/export", "203.0.113.5");
    organizer.addHeader("Authorization", "Basic eDp5");
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    new SecureCredentialsFilter(false).doFilter(organizer, response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    assertThat(response.getContentAsString()).contains("HTTPS is required");
  }

  // --- body size (SR-02) ---

  @Test
  void streamedBodyReadByteByByteIsAlsoLimited() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(new byte[BodySizeLimitFilter.LIMIT_BYTES + 1]);
    MockFilterChain chain = new MockFilterChain();
    new BodySizeLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);
    ServletInputStream in = ((HttpServletRequest) chain.getRequest()).getInputStream();

    for (int i = 0; i < BodySizeLimitFilter.LIMIT_BYTES; i++) {
      assertThat(in.read()).isZero();
    }
    assertThatThrownBy(in::read).isInstanceOf(PayloadTooLargeException.class);
  }

  @Test
  void bufferedReadsUpToTheLimitPassAndReportEndOfStream() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.2.3.4");
    request.setContent(new byte[BodySizeLimitFilter.LIMIT_BYTES]);
    MockFilterChain chain = new MockFilterChain();
    new BodySizeLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);
    ServletInputStream in = ((HttpServletRequest) chain.getRequest()).getInputStream();

    byte[] buffer = new byte[BodySizeLimitFilter.LIMIT_BYTES];
    assertThat(in.read(buffer, 0, buffer.length)).isEqualTo(BodySizeLimitFilter.LIMIT_BYTES);
    assertThat(in.read(buffer, 0, buffer.length)).isEqualTo(-1);
    assertThat(in.read()).isEqualTo(-1);
  }

  @Test
  void declaredBodyOverLimitIs413() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.2.3.4");
    request.setContent(new byte[BodySizeLimitFilter.LIMIT_BYTES + 1]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void streamedBodyOverLimitFailsWhileReading() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1; // chunked
          }
        };
    request.setContent(new byte[BodySizeLimitFilter.LIMIT_BYTES + 10]);
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);

    ServletInputStream in = ((HttpServletRequest) chain.getRequest()).getInputStream();
    assertThat(((HttpServletRequest) chain.getRequest()).getInputStream()).isSameAs(in);
    byte[] buffer = new byte[1024];
    assertThatThrownBy(
            () -> {
              while (in.read(buffer, 0, buffer.length) >= 0) {
                // keep reading
              }
            })
        .isInstanceOf(PayloadTooLargeException.class);
  }

  @Test
  void bodyAtLimitPasses() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.2.3.4");
    request.setContent(new byte[BodySizeLimitFilter.LIMIT_BYTES]);
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);

    ServletInputStream in = ((HttpServletRequest) chain.getRequest()).getInputStream();
    int total = 0;
    while (in.read() >= 0) {
      total++;
    }
    assertThat(total).isEqualTo(BodySizeLimitFilter.LIMIT_BYTES);
    assertThat(in.isFinished()).isTrue();
  }

  // --- credentials over HTTPS only (SR-03) ---

  private static int credentialsStatus(
      boolean production, String remote, boolean secure, boolean withCredentials) throws Exception {
    MockHttpServletRequest request = request("GET", "/api/registrations/export", remote);
    request.setSecure(secure);
    if (withCredentials) {
      request.addHeader("Authorization", "Basic eDp5");
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    new SecureCredentialsFilter(production).doFilter(request, response, new MockFilterChain());
    return response.getStatus();
  }

  @Test
  void credentialsOverPlainHttpFromPublicAddressAreRefused() throws Exception {
    assertThat(credentialsStatus(false, "203.0.113.5", false, true)).isEqualTo(403);
    assertThat(credentialsStatus(true, "203.0.113.5", false, true)).isEqualTo(403);
  }

  @Test
  void loopbackHttpsAndNoCredentialsAreAllowed() throws Exception {
    assertThat(credentialsStatus(true, "127.0.0.1", false, true)).isEqualTo(200);
    assertThat(credentialsStatus(true, "::1", false, true)).isEqualTo(200);
    assertThat(credentialsStatus(true, "203.0.113.5", true, true)).isEqualTo(200);
    assertThat(credentialsStatus(true, "203.0.113.5", false, false)).isEqualTo(200);
  }

  @Test
  void privateAddressesCountAsLocalOnlyOutsideProduction() throws Exception {
    assertThat(credentialsStatus(false, "172.18.0.1", false, true)).isEqualTo(200);
    assertThat(credentialsStatus(true, "172.18.0.1", false, true)).isEqualTo(403);
  }

  @Test
  void nonIpRemoteAddressIsNotLocal() {
    SecureCredentialsFilter filter = new SecureCredentialsFilter(false);

    assertThat(filter.isLocal("localhost")).isFalse();
    assertThat(filter.isLocal(null)).isFalse();
    assertThat(filter.isLocal("10.0.0.1")).isTrue();
  }
}
