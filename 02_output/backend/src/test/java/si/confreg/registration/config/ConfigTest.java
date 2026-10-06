package si.confreg.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.confreg.registration.config.AppSettings.TestClockMode;
import si.confreg.registration.testsupport.TestSettings;

/** Settings parsing, clock, test clock filter and startup guard (AR-04, AR-05, SR-04). */
class ConfigTest {

  private static AppSettings withWorkshops(String workshops) {
    AppSettings base = TestSettings.settings();
    return new AppSettings(
        base.conferenceTz(),
        base.earlyBirdDeadline(),
        base.feeEarly(),
        base.feeRegular(),
        base.vatRate(),
        workshops,
        base.rateLimitPerHour(),
        base.testClock(),
        base.mailFrom(),
        base.mailRetryInterval(),
        base.mailMaxAttempts(),
        base.organizer());
  }

  @Test
  void workshopsAreParsedInOrderWithTrimmedIdsAndTitles() {
    assertThat(withWorkshops(" W1 = First ; ;W2=Second=part;W3").workshopTitles())
        .containsExactly(entry("W1", "First"), entry("W2", "Second=part"), entry("W3", "W3"));
  }

  @Test
  void repeatedOrEmptyWorkshopIdIsRejected() {
    assertThatThrownBy(() -> withWorkshops("W1=a;W1=b").workshopTitles())
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> withWorkshops("=title").workshopTitles())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void organizerPasswordIsNotShownInText() {
    assertThat(new AppSettings.Organizer("org", "secret-value").toString())
        .doesNotContain("secret-value")
        .contains("org");
  }

  @Test
  void clockReturnsBoundTestInstantOnlyWhileBound() {
    ConferenceClock clock = new ConferenceClock();
    Instant testNow = Instant.parse("2001-02-03T04:05:06Z");
    clock.bindTestNow(testNow);
    assertThat(clock.now()).isEqualTo(testNow);
    clock.clearTestNow();
    assertThat(clock.now()).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
  }

  @Test
  void testClockFilterBindsHeaderInstantForTheRequestAndClearsIt() throws Exception {
    ConferenceClock clock = new ConferenceClock();
    TestClockFilter filter =
        new TestClockFilter(TestSettings.settings(TestClockMode.ENABLED), clock);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, "2026-07-31T21:59:59Z");
    AtomicReference<Instant> seen = new AtomicReference<>();
    FilterChain chain = (req, res) -> seen.set(clock.now());

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(seen.get()).isEqualTo(Instant.parse("2026-07-31T21:59:59Z"));
    assertThat(clock.now()).isNotEqualTo(seen.get());
  }

  @Test
  void testClockFilterRejectsInvalidInstant() throws Exception {
    TestClockFilter filter =
        new TestClockFilter(TestSettings.settings(TestClockMode.ENABLED), new ConferenceClock());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, "yesterday");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Boolean> called = new AtomicReference<>(false);

    filter.doFilter(request, response, (req, res) -> called.set(true));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString()).contains("\"status\":400");
    assertThat(called.get()).isFalse();
  }

  @Test
  void testClockHeaderIsIgnoredWhenDisabled() throws Exception {
    ConferenceClock clock = new ConferenceClock();
    TestClockFilter filter = new TestClockFilter(TestSettings.settings(), clock);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.addHeader(TestClockFilter.HEADER, "2001-02-03T04:05:06Z");
    AtomicReference<Instant> seen = new AtomicReference<>();

    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(clock.now()));

    assertThat(seen.get()).isNotEqualTo(Instant.parse("2001-02-03T04:05:06Z"));
  }

  @Test
  void productionRefusesToStartWithTestClock() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty(StartupGuard.SMTP_TLS, "true");
    StartupGuard guard =
        new StartupGuard(TestSettings.settings(TestClockMode.ENABLED), environment);
    assertThatThrownBy(guard::check)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test clock");
  }

  @Test
  void productionRefusesToStartWithoutSmtpTls() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    StartupGuard guard = new StartupGuard(TestSettings.settings(), environment);
    assertThatThrownBy(guard::check)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SMTP_TLS");
  }

  @Test
  void productionStartsWithTlsAndWithoutTestClock() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty(StartupGuard.SMTP_TLS, "true");
    new StartupGuard(TestSettings.settings(), environment).check();
  }

  @Test
  void testClockIsAllowedOutsideProduction() {
    new StartupGuard(TestSettings.settings(TestClockMode.ENABLED), new MockEnvironment()).check();
  }

  @Test
  void startupGuardRejectsInvalidWorkshopsInAnyEnvironment() {
    StartupGuard guard = new StartupGuard(withWorkshops("W1=a;W1=b"), new MockEnvironment());
    assertThatThrownBy(guard::check).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void settingsExposeTypedValues() {
    AppSettings settings = TestSettings.settings();
    assertThat(settings.vatRate()).isEqualByComparingTo(new BigDecimal("0.255"));
    assertThat(settings.mailRetryInterval()).isEqualTo(Duration.ofSeconds(30));
    assertThat(settings.testClockEnabled()).isFalse();
  }
}
