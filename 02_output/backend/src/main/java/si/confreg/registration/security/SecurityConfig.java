package si.confreg.registration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import si.confreg.registration.config.AppSettings;

/**
 * Only {@code POST /api/registrations} is public (REQ-REG-01); everything else needs the organizer
 * (SB-02). Stateless HTTP Basic, BCrypt-hashed password (SB-03), security headers (SB-10).
 */
@Configuration
public class SecurityConfig {

  static final String ORGANIZER = "ORGANIZER";

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(AbstractHttpConfigurer::disable)
        .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
        .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .hasRole(ORGANIZER))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
        .addFilterBefore(
            new CredentialTransportFilter(entryPoint), BasicAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService organizer(AppSettings settings, PasswordEncoder encoder) {
    AppSettings.Organizer organizer = settings.organizer();
    return new InMemoryUserDetailsManager(
        User.withUsername(organizer.username())
            .password(encoder.encode(organizer.password()))
            .roles(ORGANIZER)
            .build());
  }
}
