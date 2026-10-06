package si.confreg.registration.time;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AppClockTest {

  private static final Instant SYSTEM_NOW = Instant.parse("2030-05-06T07:08:09Z");
  private final AppClock clock = new AppClock(Clock.fixed(SYSTEM_NOW, ZoneOffset.UTC));

  @AfterEach
  void reset() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void usesSystemClockOutsideRequests() {
    assertThat(clock.now()).isEqualTo(SYSTEM_NOW);
  }

  @Test
  void enabledFilterSetsTestInstantForTheRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, " 2026-07-31T21:59:59.999Z ");
    AtomicReference<Instant> seen = new AtomicReference<>();
    FilterChain chain =
        (req, res) -> {
          RequestContextHolder.setRequestAttributes(
              new ServletRequestAttributes((MockHttpServletRequest) req));
          seen.set(clock.now());
        };

    new TestClockFilter(true).doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(seen.get()).isEqualTo(Instant.parse("2026-07-31T21:59:59.999Z"));
  }

  @Test
  void disabledFilterIgnoresHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, "2026-07-31T21:59:59Z");
    AtomicReference<Instant> seen = new AtomicReference<>();
    FilterChain chain =
        (req, res) -> {
          RequestContextHolder.setRequestAttributes(
              new ServletRequestAttributes((MockHttpServletRequest) req));
          seen.set(clock.now());
        };

    new TestClockFilter(false).doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(seen.get()).isEqualTo(SYSTEM_NOW);
  }

  @Test
  void invalidTestInstantIsBadRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, "yesterday");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Boolean> called = new AtomicReference<>(false);

    new TestClockFilter(true).doFilter(request, response, (req, res) -> called.set(true));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    assertThat(response.getContentAsString()).contains("\"status\":400").contains("X-Test-Now");
    assertThat(called.get()).isFalse();
  }
}
