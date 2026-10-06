package si.confreg.registration.time;

import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import si.confreg.registration.config.AppProperties;

/** System clock (UTC) and, when enabled, the per-request test clock (SR-04). */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

  @Bean
  Clock systemClock() {
    return Clock.systemUTC();
  }

  @Bean
  FilterRegistrationBean<TestClockFilter> testClockFilter(AppProperties properties) {
    FilterRegistrationBean<TestClockFilter> registration =
        new FilterRegistrationBean<>(new TestClockFilter(properties.testClockEnabled()));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
