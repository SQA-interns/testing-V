package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegistrationNumberGeneratorTest {

  private final RegistrationNumberGenerator generator = new RegistrationNumberGenerator();

  @Test
  void numberHasPrefixAndTenCrockfordCharacters() {
    for (int i = 0; i < 1_000; i++) {
      assertThat(generator.next()).matches("CR-[0-9ABCDEFGHJKMNPQRSTVWXYZ]{10}");
    }
  }

  @Test
  void numbersDoNotRepeatInPractice() {
    Set<String> numbers = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      numbers.add(generator.next());
    }
    assertThat(numbers).hasSize(10_000);
  }

  @Test
  void payerTypeValuesMatchTheApi() {
    assertThat(PayerType.PRIVATE.value()).isEqualTo("private");
    assertThat(PayerType.COMPANY.value()).isEqualTo("company");
    assertThat(PayerType.fromValue("company")).contains(PayerType.COMPANY);
    assertThat(PayerType.fromValue("student")).isEmpty();
  }
}
