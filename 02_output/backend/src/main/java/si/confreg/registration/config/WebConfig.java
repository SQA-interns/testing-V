package si.confreg.registration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.infrastructure.clock.TestClockFilter;
import si.confreg.registration.infrastructure.web.BodySizeLimitFilter;
import si.confreg.registration.infrastructure.web.RateLimitFilter;
import si.confreg.registration.infrastructure.web.RateLimiter;

/**
 * Settings binding and the servlet filters that run before Spring Security (order -100): test clock
 * first (so every later step sees the request's time), then body size, then rate limit.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig {

  @Bean
  FilterRegistrationBean<TestClockFilter> testClockFilter(AppProperties properties) {
    FilterRegistrationBean<TestClockFilter> registration =
        new FilterRegistrationBean<>(new TestClockFilter(properties));
    registration.setOrder(-300);
    return registration;
  }

  @Bean
  FilterRegistrationBean<BodySizeLimitFilter> bodySizeLimitFilter() {
    FilterRegistrationBean<BodySizeLimitFilter> registration =
        new FilterRegistrationBean<>(new BodySizeLimitFilter());
    registration.setOrder(-250);
    return registration;
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimiter limiter) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(new RateLimitFilter(limiter));
    registration.setOrder(-200);
    return registration;
  }
}
