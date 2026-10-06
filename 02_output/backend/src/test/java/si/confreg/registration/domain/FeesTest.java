package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FeesTest {

  @ParameterizedTest
  @CsvSource({
    "240.00, 0.22, 52.80, 292.80",
    "300.00, 0.22, 66.00, 366.00",
    "100.10, 0.25, 25.03, 125.13", // 25.025 rounds half-up
    "200.30, 0.25, 50.08, 250.38", // 50.075 rounds half-up
    "0.00, 0.22, 0.00, 0.00",
    "10.00, 0, 0.00, 10.00"
  })
  void computesVatHalfUpAndGross(String net, String rate, String vat, String gross) {
    Fees fees = Fees.fromNet(new BigDecimal(net), new BigDecimal(rate));

    assertThat(fees.netFee()).isEqualByComparingTo(net);
    assertThat(fees.vat()).isEqualByComparingTo(vat);
    assertThat(fees.grossFee()).isEqualByComparingTo(gross);
    assertThat(fees.vat().scale()).isEqualTo(2);
    assertThat(fees.grossFee().scale()).isEqualTo(2);
  }
}
