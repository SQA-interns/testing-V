package si.confreg.registration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import si.confreg.registration.application.TimeSource;

/**
 * Access control and protective filters (docs/02_specification.md 4.4): public registration and
 * workshop list (D-29, D-28), organizer HTTP Basic for everything else under /api (SB-02), BCrypt
 * hash of the organizer password (SB-03), rate limit (SB-06), body size (SR-02), transport rule
 * (SR-03), security headers (SB-10).
 */
@Configuration
public class SecurityConfiguration {

  private static final String REALM = "confreg";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService organizer(
      @Value("${app.organizer.username}") String username,
      @Value("${app.organizer.password}") String password,
      PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("ORGANIZER").build());
  }

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      @Value("${app.rate-limit-per-hour}") int rateLimitPerHour,
      TimeSource time,
      Environment environment) {
    AuthenticationEntryPoint unauthorized =
        (request, response, exception) -> {
          response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
          JsonError.write(response, 401, "unauthorized");
        };
    boolean production = environment.matchesProfiles("prod");
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(basic -> basic.realmName(REALM).authenticationEntryPoint(unauthorized))
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/workshops")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .hasRole("ORGANIZER")
                    .anyRequest()
                    .permitAll())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
        .addFilterBefore(
            new RateLimitFilter(rateLimitPerHour, time), BasicAuthenticationFilter.class)
        .addFilterBefore(new RequestSizeFilter(), BasicAuthenticationFilter.class)
        .addFilterBefore(new TransportFilter(production), BasicAuthenticationFilter.class);
    return http.build();
  }
}
