package si.confreg.registration.domain;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Registration numbers {@code REG-} plus 10 Crockford Base32 characters (spec 4.3): random, so a
 * number reveals neither count nor order.
 */
public final class RegistrationNumbers {

  static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  static final String PREFIX = "REG-";
  static final int LENGTH = 10;

  /** Pattern of a well-formed registration number. */
  public static final Pattern FORMAT = Pattern.compile("^REG-[0-9A-HJKMNP-TV-Z]{10}$");

  private final RandomGenerator random;

  public RegistrationNumbers() {
    this(new SecureRandom());
  }

  public RegistrationNumbers(RandomGenerator random) {
    this.random = random;
  }

  public String next() {
    StringBuilder number = new StringBuilder(PREFIX.length() + LENGTH).append(PREFIX);
    for (int i = 0; i < LENGTH; i++) {
      number.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return number.toString();
  }

  public static boolean isWellFormed(String candidate) {
    return candidate != null && FORMAT.matcher(candidate).matches();
  }
}
