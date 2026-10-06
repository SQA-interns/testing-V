package si.confreg.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.application.RateLimiter;

/** Unit tests of the request filters (SR-02, SR-03, AR-05, AC-001-12). */
class WebFiltersTest {

  private static final Instant SYSTEM = Instant.parse("2032-02-02T02:02:02Z");

  // ------------------------------------------------------------------ test clock

  @Test
  void testClockUsesHeaderOnlyDuringTheRequest() throws Exception {
    RequestTimeSource time = new RequestTimeSource(true, Clock.fixed(SYSTEM, ZoneOffset.UTC));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(RequestTimeSource.HEADER, " 2026-07-31T21:59:59Z ");
    AtomicReference<Instant> seen = new AtomicReference<>();

    time.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(time.now()));

    assertThat(seen.get()).isEqualTo(Instant.parse("2026-07-31T21:59:59Z"));
    assertThat(time.now()).as("after the request").isEqualTo(SYSTEM);
  }

  @Test
  void testClockIgnoresHeaderWhenDisabled() throws Exception {
    RequestTimeSource time = new RequestTimeSource(false, Clock.fixed(SYSTEM, ZoneOffset.UTC));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(RequestTimeSource.HEADER, "not-a-time");
    AtomicReference<Instant> seen = new AtomicReference<>();
    MockHttpServletResponse response = new MockHttpServletResponse();

    time.doFilter(request, response, (req, res) -> seen.set(time.now()));

    assertThat(seen.get()).isEqualTo(SYSTEM);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void testClockRejectsMalformedHeader() throws Exception {
    RequestTimeSource time = new RequestTimeSource(true, Clock.fixed(SYSTEM, ZoneOffset.UTC));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(RequestTimeSource.HEADER, "2026-07-31 21:59");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    time.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(chain.getRequest()).as("chain not called").isNull();
  }

  @Test
  void testClockWithoutHeaderUsesSystemTime() throws Exception {
    RequestTimeSource time = new RequestTimeSource(true, Clock.fixed(SYSTEM, ZoneOffset.UTC));
    AtomicReference<Instant> seen = new AtomicReference<>();

    time.doFilter(
        new MockHttpServletRequest("GET", "/api/workshops"),
        new MockHttpServletResponse(),
        (req, res) -> seen.set(time.now()));

    assertThat(seen.get()).isEqualTo(SYSTEM);
    assertThat(new RequestTimeSource(false).now()).isNotNull();
  }

  // ------------------------------------------------------------------ body limit

  @Test
  void bodyLimitRejectsLargeContentLength() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES + 1]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new RequestBodyLimitFilter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void bodyLimitAllowsBodyAtTheLimit() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Integer> read = new AtomicReference<>();

    new RequestBodyLimitFilter()
        .doFilter(
            request,
            response,
            (req, res) ->
                read.set(((HttpServletRequest) req).getInputStream().readAllBytes().length));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(read.get()).isEqualTo(RequestBodyLimitFilter.MAX_BODY_BYTES);
  }

  @Test
  void bodyLimitStopsBodyWithoutContentLength() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }

          @Override
          public int getContentLength() {
            return -1;
          }
        };
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES + 10]);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestBodyLimitFilter()
        .doFilter(
            request,
            response,
            (req, res) -> {
              try (InputStream in = ((HttpServletRequest) req).getInputStream()) {
                in.readAllBytes();
              } catch (IOException e) {
                assertThat(RequestBodyLimitFilter.exceeded((HttpServletRequest) req)).isTrue();
                throw new IllegalStateException("body read failed", e);
              }
            });

    assertThat(response.getStatus()).isEqualTo(413);
  }

  @Test
  void exceededIsFalseForPlainRequests() {
    assertThat(RequestBodyLimitFilter.exceeded(new MockHttpServletRequest())).isFalse();
  }

  private static MockHttpServletRequest chunked(int bytes) {
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
  void chunkedBodyExactlyAtTheLimitIsReadInFull() throws Exception {
    AtomicReference<Integer> read = new AtomicReference<>();
    AtomicReference<Boolean> exceeded = new AtomicReference<>();
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestBodyLimitFilter()
        .doFilter(
            chunked(RequestBodyLimitFilter.MAX_BODY_BYTES),
            response,
            (req, res) -> {
              InputStream in = ((HttpServletRequest) req).getInputStream();
              byte[] buffer = new byte[1000];
              int total = 0;
              for (int n = in.read(buffer, 0, buffer.length);
                  n > 0;
                  n = in.read(buffer, 0, buffer.length)) {
                total += n;
              }
              read.set(total);
              exceeded.set(RequestBodyLimitFilter.exceeded((HttpServletRequest) req));
            });

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(read.get()).isEqualTo(RequestBodyLimitFilter.MAX_BODY_BYTES);
    assertThat(exceeded.get()).isFalse();
  }

  @Test
  void chunkedBodyReadByteByByteStopsAfterTheLimit() throws Exception {
    AtomicReference<Integer> read = new AtomicReference<>(0);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestBodyLimitFilter()
        .doFilter(
            chunked(RequestBodyLimitFilter.MAX_BODY_BYTES + 1),
            response,
            (req, res) -> {
              InputStream in = ((HttpServletRequest) req).getInputStream();
              try {
                while (in.read() >= 0) {
                  read.set(read.get() + 1);
                }
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            });

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(read.get()).isEqualTo(RequestBodyLimitFilter.MAX_BODY_BYTES);
  }

  // ------------------------------------------------------------------ SR-03

  @ParameterizedTest
  @CsvSource({
    "127.0.0.1, true",
    "127.10.20.30, true",
    "::1, true",
    "0:0:0:0:0:0:0:1, true",
    "[::1], true",
    "10.0.0.1, false",
    "172.17.0.1, false",
    "::ffff:10.0.0.1, false",
    "localhost, false",
    "example.org, false",
    "'', false"
  })
  void loopbackDetectionUsesLiteralAddressesOnly(String address, boolean loopback) {
    assertThat(InsecureCredentialsFilter.isLoopback(address)).isEqualTo(loopback);
  }

  @Test
  void loopbackOfNullIsFalse() {
    assertThat(InsecureCredentialsFilter.isLoopback(null)).isFalse();
  }

  private static MockHttpServletResponse credentialsRequest(
      boolean plainHttpAllowed, String remote, boolean secure) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/x");
    request.setRemoteAddr(remote);
    request.setSecure(secure);
    request.addHeader("Authorization", "Basic eDp5");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new InsecureCredentialsFilter(plainHttpAllowed).doFilter(request, response, chain);
    response.setHeader("X-Chain-Called", Boolean.toString(chain.getRequest() != null));
    return response;
  }

  @Test
  void credentialsOverPlainHttpFromRemoteClientAreRejected() throws Exception {
    MockHttpServletResponse response = credentialsRequest(false, "10.1.2.3", false);
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader("X-Chain-Called")).isEqualTo("false");
  }

  @Test
  void credentialsOverHttpsOrFromLoopbackOrInLocalProfileAreAccepted() throws Exception {
    for (MockHttpServletResponse response :
        new MockHttpServletResponse[] {
          credentialsRequest(false, "10.1.2.3", true),
          credentialsRequest(false, "127.0.0.1", false),
          credentialsRequest(true, "10.1.2.3", false)
        }) {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeader("X-Chain-Called")).as("request passed on").isEqualTo("true");
    }
  }

  @Test
  void requestsWithoutCredentialsAreNotAffected() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setRemoteAddr("10.1.2.3");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new InsecureCredentialsFilter(false).doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(200);
  }

  // ------------------------------------------------------------------ rate limit

  private final Instant now = Instant.parse("2032-01-01T00:00:00Z");
  private final RateLimiter limiter = new RateLimiter(2, () -> now);
  private final RateLimitFilter rateLimit = new RateLimitFilter(limiter);

  private MockHttpServletResponse call(
      String method, String path, String client, String auth, int status) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(client);
    if (auth != null) {
      request.addHeader("Authorization", auth);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    rateLimit.doFilter(
        request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(status));
    return response;
  }

  @Test
  void registrationBucketIsPerClient() throws Exception {
    assertThat(call("POST", "/api/registrations", "1.1.1.1", null, 201).getStatus()).isEqualTo(201);
    assertThat(call("POST", "/api/registrations", "1.1.1.1", null, 422).getStatus()).isEqualTo(422);
    MockHttpServletResponse limited = call("POST", "/api/registrations", "1.1.1.1", null, 201);

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("3600");
    assertThat(call("POST", "/api/registrations", "2.2.2.2", null, 201).getStatus()).isEqualTo(201);
  }

  @Test
  void workshopBucketIsSeparateAndOtherPathsAreNotLimited() throws Exception {
    call("POST", "/api/registrations", "1.1.1.1", null, 201);
    call("POST", "/api/registrations", "1.1.1.1", null, 201);

    assertThat(call("GET", "/api/workshops", "1.1.1.1", null, 200).getStatus()).isEqualTo(200);
    assertThat(call("GET", "/api/workshops", "1.1.1.1", null, 200).getStatus()).isEqualTo(200);
    assertThat(call("GET", "/api/workshops", "1.1.1.1", null, 200).getStatus()).isEqualTo(429);
    for (int i = 0; i < 5; i++) {
      assertThat(call("GET", "/api/registrations/REG-1", "1.1.1.1", null, 401).getStatus())
          .isEqualTo(401);
      assertThat(call("GET", "/api/registrations", "1.1.1.1", null, 401).getStatus())
          .isEqualTo(401);
    }
  }

  @Test
  void failedLoginsBlockFurtherCredentialsFromThatClient() throws Exception {
    assertThat(call("GET", "/api/registrations/REG-1", "3.3.3.3", "Basic a", 401).getStatus())
        .isEqualTo(401);
    assertThat(call("GET", "/api/registrations/REG-1", "3.3.3.3", "Basic a", 401).getStatus())
        .isEqualTo(401);

    assertThat(call("GET", "/api/registrations/REG-1", "3.3.3.3", "Basic b", 200).getStatus())
        .isEqualTo(429);
    assertThat(call("GET", "/api/registrations/REG-1", "4.4.4.4", "Basic b", 200).getStatus())
        .isEqualTo(200);
    assertThat(call("POST", "/api/registrations", "3.3.3.3", null, 201).getStatus())
        .as("public registration without credentials still works")
        .isEqualTo(201);
  }

  @Test
  void successfulLoginsAreNotCounted() throws Exception {
    for (int i = 0; i < 5; i++) {
      assertThat(call("GET", "/api/registrations/REG-1", "5.5.5.5", "Basic ok", 200).getStatus())
          .isEqualTo(200);
    }
  }
}
