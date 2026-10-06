package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PayerTypeTest {

  @Test
  void mapsApiValues() {
    assertThat(PayerType.fromValue("private")).contains(PayerType.PRIVATE);
    assertThat(PayerType.fromValue("company")).contains(PayerType.COMPANY);
    assertThat(PayerType.PRIVATE.value()).isEqualTo("private");
  }

  @Test
  void rejectsOtherValues() {
    assertThat(PayerType.fromValue("Company")).isEmpty();
    assertThat(PayerType.fromValue(null)).isEmpty();
    assertThat(PayerType.fromValue("")).isEmpty();
  }
}
