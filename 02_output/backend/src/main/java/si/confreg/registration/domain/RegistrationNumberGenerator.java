package si.confreg.registration.domain;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Random registration numbers {@code CR-} plus 10 Crockford base-32 characters. */
@Component
public class RegistrationNumberGenerator {

  private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
  private static final int LENGTH = 10;

  private final SecureRandom random = new SecureRandom();

  public String next() {
    StringBuilder number = new StringBuilder("CR-");
    for (int i = 0; i < LENGTH; i++) {
      number.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return number.toString();
  }
}
