package si.confreg.registration.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.application.TimeSource;

class FiltersTest {

  /** Settable time for the rate-limit window. */
  private static final class FixedTime implements TimeSource {
    private Instant now;

    FixedTime(Instant now) {
      this.now = now;
    }

    @Override
    public Instant now() {
      return now;
    }

    @Override
    public Instant systemNow() {
      return now;
    }
  }

  /** A request whose body length is unknown (chunked transfer). */
  private static final class ChunkedRequest extends MockHttpServletRequest {
    ChunkedRequest(byte[] body) {
      super("POST", "/api/registrations");
      setContent(body);
    }

    @Override
    public long getContentLengthLong() {
      return -1;
    }
  }

  private static MockHttpServletResponse run(Filter filter, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  private static MockHttpServletRequest api(String client) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setRemoteAddr(client);
    return request;
  }

  // ---- RateLimitFilter (SB-06) ----

  @Test
  void rateLimitAllowsLimitThenRejectsWithRetryAfter() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(2, new FixedTime(Instant.parse("2026-10-06T10:59:30Z")));

    assertThat(run(filter, api("10.0.0.1")).getStatus()).isEqualTo(200);
    assertThat(run(filter, api("10.0.0.1")).getStatus()).isEqualTo(200);
    MockHttpServletResponse limited = run(filter, api("10.0.0.1"));

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("30");
    assertThat(limited.getContentAsString()).isEqualTo("{\"error\":\"rate_limited\"}");
  }

  @Test
  void rateLimitIsPerClientAndResetsInNextHour() throws Exception {
    FixedTime time = new FixedTime(Instant.parse("2026-10-06T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(1, time);

    assertThat(run(filter, api("10.0.0.1")).getStatus()).isEqualTo(200);
    assertThat(run(filter, api("10.0.0.2")).getStatus()).isEqualTo(200);
    assertThat(run(filter, api("10.0.0.1")).getStatus()).isEqualTo(429);
    time.now = Instant.parse("2026-10-06T11:00:00Z");
    assertThat(run(filter, api("10.0.0.1")).getStatus()).isEqualTo(200);
  }

  @Test
  void rateLimitIgnoresNonApiPaths() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(0, new FixedTime(Instant.EPOCH));
    assertThat(run(filter, new MockHttpServletRequest("GET", "/index.html")).getStatus())
        .isEqualTo(200);
  }

  // ---- RequestSizeFilter (SR-02) ----

  @Test
  void bodyAtLimitPassesAndOverLimitIsRejected() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter();
    MockHttpServletRequest atLimit = api("127.0.0.1");
    atLimit.setContent(new byte[RequestSizeFilter.MAX_BODY_BYTES]);
    MockHttpServletRequest over = api("127.0.0.1");
    over.setContent(new byte[RequestSizeFilter.MAX_BODY_BYTES + 1]);

    assertThat(run(filter, atLimit).getStatus()).isEqualTo(200);
    MockHttpServletResponse rejected = run(filter, over);
    assertThat(rejected.getStatus()).isEqualTo(413);
    assertThat(rejected.getContentAsString()).isEqualTo("{\"error\":\"payload_too_large\"}");
  }

  @Test
  void chunkedBodyIsMeasuredAndReplayed() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter();
    String json = "{\"firstName\":\"Špela\"}";
    AtomicReference<String> replayed = new AtomicReference<>();

    filter.doFilter(
        new ChunkedRequest(json.getBytes(StandardCharsets.UTF_8)),
        new MockHttpServletResponse(),
        (req, res) ->
            replayed.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)));

    assertThat(replayed.get()).isEqualTo(json);
    assertThat(
            run(filter, new ChunkedRequest(new byte[RequestSizeFilter.MAX_BODY_BYTES + 1]))
                .getStatus())
        .isEqualTo(413);
  }

  // ---- TransportFilter (SR-03, D-27) ----

  @ParameterizedTest
  @CsvSource({
    // production, secure, client, expected status
    "true, false, 127.0.0.1, 200",
    "true, false, ::1, 200",
    "true, true, 203.0.113.9, 200",
    "true, false, 203.0.113.9, 403",
    "true, false, 172.18.0.1, 403",
    "false, false, 172.18.0.1, 200",
    "false, false, 192.168.1.5, 200",
    "false, false, 203.0.113.9, 403",
    "false, false, example.com, 403"
  })
  void credentialsOnlyOverAcceptableTransport(
      boolean production, boolean secure, String client, int expected) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/x");
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    request.setSecure(secure);
    request.setRemoteAddr(client);

    MockHttpServletResponse response = run(new TransportFilter(production), request);

    assertThat(response.getStatus()).isEqualTo(expected);
    if (expected == 403) {
      assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"insecure_transport\"}");
    }
  }

  @Test
  void requestWithoutCredentialsIsNotAffected() throws Exception {
    assertThat(run(new TransportFilter(true), api("203.0.113.9")).getStatus()).isEqualTo(200);
  }
}
