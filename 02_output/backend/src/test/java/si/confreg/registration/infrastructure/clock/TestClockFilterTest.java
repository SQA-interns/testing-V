package si.confreg.registration.infrastructure.clock;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.application.TestSettings;

class TestClockFilterTest {

  private final RequestTimeSource timeSource = new RequestTimeSource();

  private Instant timeSeenByRequest(boolean enabled, String header) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registration-options");
    if (header != null) {
      request.addHeader(TestClockFilter.HEADER, header);
    }
    AtomicReference<Instant> seen = new AtomicReference<>();
    FilterChain chain = (req, res) -> seen.set(timeSource.now());
    new TestClockFilter(TestSettings.with("testClock", enabled ? "enabled" : "disabled"))
        .doFilter(request, new MockHttpServletResponse(), chain);
    return seen.get();
  }

  @Test
  void enabledClockUsesHeaderForThatRequestOnly() throws Exception {
    Instant fixed = Instant.parse("2026-07-31T21:59:59Z");

    assertThat(timeSeenByRequest(true, "2026-07-31T21:59:59Z")).isEqualTo(fixed);
    assertThat(timeSource.now()).isNotEqualTo(fixed);
  }

  @Test
  void disabledClockIgnoresHeader() throws Exception {
    Instant before = Instant.now();

    assertThat(timeSeenByRequest(false, "2000-01-01T00:00:00Z")).isAfterOrEqualTo(before);
  }

  @Test
  void withoutHeaderSystemTimeIsUsed() throws Exception {
    Instant before = Instant.now();

    assertThat(timeSeenByRequest(true, null)).isAfterOrEqualTo(before);
  }

  @Test
  void invalidHeaderIs400() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.addHeader(TestClockFilter.HEADER, "yesterday");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new TestClockFilter(TestSettings.with("testClock", "enabled"))
        .doFilter(
            request,
            response,
            (req, res) -> {
              throw new AssertionError("chain must not run");
            });

    assertThat(response.getStatus()).isEqualTo(400);
  }
}
