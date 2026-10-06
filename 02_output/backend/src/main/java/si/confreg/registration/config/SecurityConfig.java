package si.confreg.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import si.confreg.registration.web.Problems;

/**
 * Stateless HTTP Basic for the organizer; only the registration POST and the workshop list are
 * public (spec 7.1, 7.5; SB-02, SB-03, SB-10).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

  static final String ROLE_ORGANIZER = "ORGANIZER";
  static final String REALM = "confreg";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** One organizer; the password is hashed here and the plain value is not kept in a bean. */
  @Bean
  UserDetailsService organizer(
      Environment environment, PasswordEncoder encoder, StartupGuards guards) {
    return new InMemoryUserDetailsManager(
        User.withUsername(environment.getRequiredProperty("app.organizer.username"))
            .password(encoder.encode(environment.getRequiredProperty("app.organizer.password")))
            .roles(ROLE_ORGANIZER)
            .build());
  }

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .httpBasic(
            basic ->
                basic
                    .realmName(REALM)
                    .authenticationEntryPoint(
                        (request, response, e) -> {
                          response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
                          Problems.write(response, HttpStatus.UNAUTHORIZED);
                        }))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, e) -> {
                          response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
                          Problems.write(response, HttpStatus.UNAUTHORIZED);
                        })
                    .accessDeniedHandler(
                        (request, response, e) -> Problems.write(response, HttpStatus.FORBIDDEN)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/registrations")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/workshops")
                    .permitAll()
                    .requestMatchers("/error", "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .hasRole(ROLE_ORGANIZER))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)));
    return http.build();
  }
}
