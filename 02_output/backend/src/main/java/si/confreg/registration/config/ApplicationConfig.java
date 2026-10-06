package si.confreg.registration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import si.confreg.registration.application.ConfirmationSender;
import si.confreg.registration.application.RateLimiter;
import si.confreg.registration.application.RegisterParticipant;
import si.confreg.registration.application.RegistrationQuery;
import si.confreg.registration.application.RegistrationStore;
import si.confreg.registration.application.RegistrationValidator;
import si.confreg.registration.domain.FeePolicy;
import si.confreg.registration.domain.RegistrationNumbers;
import si.confreg.registration.domain.WorkshopCatalog;
import si.confreg.registration.mail.SmtpConfirmationSender;
import si.confreg.registration.web.InsecureCredentialsFilter;
import si.confreg.registration.web.RateLimitFilter;
import si.confreg.registration.web.RequestBodyLimitFilter;
import si.confreg.registration.web.RequestTimeSource;

/** Wiring of the use cases, adapters and request filters (spec 2). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

  /** Profiles whose environments listen on 127.0.0.1 only (environments.md, D-13, SR-04). */
  static final Profiles LOCAL_OR_TEST = Profiles.of("local", "test");

  // Filter order: all before Spring Security (-100), test clock first.
  static final int ORDER_TIME = -200;
  static final int ORDER_BODY_LIMIT = -190;
  static final int ORDER_CREDENTIALS = -180;
  static final int ORDER_RATE_LIMIT = -170;

  @Bean
  StartupGuards startupGuards(AppProperties properties, Environment environment) {
    StartupGuards guards = new StartupGuards(properties, environment);
    guards.check();
    return guards;
  }

  @Bean
  WorkshopCatalog workshopCatalog(AppProperties properties) {
    return WorkshopCatalog.parse(properties.workshops());
  }

  @Bean
  FeePolicy feePolicy(AppProperties properties) {
    return new FeePolicy(
        properties.conferenceTz(),
        properties.earlyBirdDeadline(),
        properties.feeEarly(),
        properties.feeRegular(),
        properties.vatRate());
  }

  @Bean
  RequestTimeSource timeSource(AppProperties properties, StartupGuards guards) {
    return new RequestTimeSource(properties.testClockEnabled());
  }

  @Bean
  RateLimiter rateLimiter(AppProperties properties, RequestTimeSource timeSource) {
    return new RateLimiter(properties.rateLimitPerHour(), timeSource);
  }

  @Bean
  ConfirmationSender confirmationSender(
      JavaMailSender mailSender, AppProperties properties, WorkshopCatalog workshops) {
    return new SmtpConfirmationSender(mailSender, properties.mailFrom(), workshops);
  }

  @Bean
  RegisterParticipant registerParticipant(
      WorkshopCatalog workshops,
      FeePolicy feePolicy,
      RegistrationStore store,
      ConfirmationSender confirmationSender,
      RequestTimeSource timeSource) {
    return new RegisterParticipant(
        new RegistrationValidator(workshops),
        feePolicy,
        new RegistrationNumbers(),
        store,
        confirmationSender,
        timeSource);
  }

  @Bean
  RegistrationQuery registrationQuery(RegistrationStore store) {
    return new RegistrationQuery(store);
  }

  @Bean
  FilterRegistrationBean<RequestTimeSource> timeSourceFilter(RequestTimeSource timeSource) {
    return registration(timeSource, ORDER_TIME);
  }

  @Bean
  FilterRegistrationBean<RequestBodyLimitFilter> bodyLimitFilter() {
    return registration(new RequestBodyLimitFilter(), ORDER_BODY_LIMIT);
  }

  @Bean
  FilterRegistrationBean<InsecureCredentialsFilter> insecureCredentialsFilter(
      Environment environment) {
    return registration(
        new InsecureCredentialsFilter(environment.acceptsProfiles(LOCAL_OR_TEST)),
        ORDER_CREDENTIALS);
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimiter rateLimiter) {
    return registration(new RateLimitFilter(rateLimiter), ORDER_RATE_LIMIT);
  }

  private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> registration(
      T filter, int order) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/api/*");
    registration.setOrder(order);
    return registration;
  }
}
