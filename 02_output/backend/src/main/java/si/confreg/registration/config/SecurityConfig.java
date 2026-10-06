package si.confreg.registration.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import si.confreg.registration.infrastructure.web.ProblemWriter;
import si.confreg.registration.infrastructure.web.SecureCredentialsFilter;

/**
 * Authentication and authorization (security-requirements.md, SB-02, SB-03, SB-10, D-16): only
 * registration, registration options and health are public; everything else needs the organizer.
 */
@Configuration
public class SecurityConfig {

  static final int MIN_PASSWORD_LENGTH = 16;
  private static final String REALM = "registration";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment)
      throws Exception {
    boolean production = environment.acceptsProfiles(Profiles.of("prod"));
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/registration-options")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(basic -> basic.realmName(REALM).authenticationEntryPoint(entryPoint()))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint()))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
        .addFilterBefore(new SecureCredentialsFilter(production), BasicAuthenticationFilter.class);
    return http.build();
  }

  private static AuthenticationEntryPoint entryPoint() {
    return (request, response, exception) -> {
      response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
      ProblemWriter.write(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "Unauthorized",
          "Organizer credentials are required.");
    };
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** The single organizer account; the password is kept only as a BCrypt hash (SB-03). */
  @Bean
  UserDetailsService organizer(
      @Value("${ORGANIZER_USERNAME:}") String username,
      @Value("${ORGANIZER_PASSWORD:}") String password,
      PasswordEncoder encoder) {
    if (username.isBlank()) {
      throw new IllegalStateException("Missing required setting ORGANIZER_USERNAME");
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "Setting ORGANIZER_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("ORGANIZER").build());
  }
}
