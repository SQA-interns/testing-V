package si.confreg.registration.config;

import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Builds the organizer login from {@code ORGANIZER_USERNAME} and {@code ORGANIZER_PASSWORD}. The
 * plain password is hashed at once and not kept (SB-03); missing or weak values stop startup.
 */
public final class OrganizerAccount {

  public static final String ROLE = "ORGANIZER";
  static final int MIN_PASSWORD_LENGTH = 16;

  private OrganizerAccount() {}

  public static UserDetails fromEnvironment(Environment environment, PasswordEncoder encoder) {
    String username = environment.getProperty("organizer.username");
    String password = environment.getProperty("organizer.password");
    if (username == null || username.isBlank()) {
      throw new IllegalStateException("ORGANIZER_USERNAME must be set");
    }
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "ORGANIZER_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    return User.withUsername(username).password(encoder.encode(password)).roles(ROLE).build();
  }
}
