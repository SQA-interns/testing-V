package si.confreg.registration.clock;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TestClockTest {

  private static final Instant FIXED = Instant.parse("2026-07-31T21:59:59Z");

  private static Instant seenDuringRequest(
      boolean enabled, String header, Function<ApplicationTimeSource, Instant> read)
      throws Exception {
    TestClockSettings settings = new TestClockSettings(enabled);
    ApplicationTimeSource time = new ApplicationTimeSource(settings);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    if (header != null) {
      request.addHeader("X-Test-Now", header);
    }
    AtomicReference<Instant> seen = new AtomicReference<>();
    new TestClockFilter(settings)
        .doFilter(
            request,
            new MockHttpServletResponse(),
            new MockFilterChain() {
              @Override
              public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(read.apply(time));
              }
            });
    return seen.get();
  }

  private static boolean nearSystemTime(Instant instant) {
    return Duration.between(instant, Instant.now()).abs().compareTo(Duration.ofMinutes(1)) < 0;
  }

  @Test
  void enabledClockUsesHeaderForTheRequestOnly() throws Exception {
    assertThat(seenDuringRequest(true, FIXED.toString(), ApplicationTimeSource::now))
        .isEqualTo(FIXED);
    assertThat(nearSystemTime(new ApplicationTimeSource(new TestClockSettings(true)).now()))
        .isTrue();
  }

  @Test
  void disabledClockIgnoresHeader() throws Exception {
    assertThat(
            nearSystemTime(seenDuringRequest(false, FIXED.toString(), ApplicationTimeSource::now)))
        .isTrue();
  }

  @Test
  void enabledClockWithoutHeaderUsesSystemTime() throws Exception {
    assertThat(nearSystemTime(seenDuringRequest(true, null, ApplicationTimeSource::now))).isTrue();
  }

  @Test
  void systemNowIgnoresTestClock() throws Exception {
    assertThat(
            nearSystemTime(
                seenDuringRequest(true, FIXED.toString(), ApplicationTimeSource::systemNow)))
        .isTrue();
  }

  @Test
  void malformedHeaderIsRejected() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader("X-Test-Now", "yesterday");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new TestClockFilter(new TestClockSettings(true)).doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"invalid_test_clock\"}");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void malformedHeaderIsIgnoredWhenDisabled() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader("X-Test-Now", "yesterday");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new TestClockFilter(new TestClockSettings(false)).doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }
}
