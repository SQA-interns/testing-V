package si.confreg.registration.security;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import si.confreg.registration.config.AppProperties;
import si.confreg.registration.config.OrganizerAccount;

/** Access rules, filters and headers (docs/02_specification.md 6.2 to 6.7). */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService organizers(Environment environment, PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(OrganizerAccount.fromEnvironment(environment, encoder));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties, Clock clock)
      throws Exception {
    AuthenticationEntryPoint unauthorized =
        (request, response, exception) -> {
          response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
          ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Authentication required.");
        };
    AccessDeniedHandler forbidden =
        (request, response, exception) ->
            ProblemResponses.write(response, HttpStatus.FORBIDDEN, "Access denied.");

    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .formLogin(form -> form.disable())
        .logout(logout -> logout.disable())
        .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/registrations")
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
                    .hasRole(OrganizerAccount.ROLE))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .addFilterBefore(
            new RateLimitFilter(properties.rateLimitPerHour(), clock), DisableEncodeUrlFilter.class)
        .addFilterBefore(new BodySizeLimitFilter(), BasicAuthenticationFilter.class)
        .addFilterBefore(
            new InsecureCredentialsFilter(properties.insecureAuthAllowed()),
            BasicAuthenticationFilter.class);
    return http.build();
  }
}
