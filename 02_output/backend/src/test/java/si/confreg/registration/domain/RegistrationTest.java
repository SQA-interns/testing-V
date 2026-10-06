package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationTest {

  private static final Price PRICE =
      new Price(new BigDecimal("8.20"), new BigDecimal("1.80"), new BigDecimal("10.00"));
  private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");

  @Test
  void companyRegistrationKeepsCompanyFieldsAndWorkshop() {
    RegistrationInput input =
        new RegistrationInput(
            " Žiga ", "Šuštar", "z@c.si", "company", "Primer", "Čopova 1", "SI1", List.of("K1"));

    Registration registration = Registration.create("REG-000042", input, PRICE, NOW);

    assertThat(registration.getRegistrationNumber()).isEqualTo("REG-000042");
    assertThat(registration.getFirstName()).isEqualTo("Žiga");
    assertThat(registration.getPayerType()).isEqualTo(PayerType.COMPANY);
    assertThat(registration.getCompanyName()).isEqualTo("Primer");
    assertThat(registration.getCompanyAddress()).isEqualTo("Čopova 1");
    assertThat(registration.getCompanyVatId()).isEqualTo("SI1");
    assertThat(registration.getWorkshop()).isEqualTo("K1");
    assertThat(registration.getNetFee()).isEqualByComparingTo("8.20");
    assertThat(registration.getVat()).isEqualByComparingTo("1.80");
    assertThat(registration.getGrossFee()).isEqualByComparingTo("10.00");
    assertThat(registration.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void privateRegistrationHasNoCompanyFieldsOrWorkshop() {
    RegistrationInput input =
        new RegistrationInput("Ana", "Kovač", "a@c.si", "private", null, null, null, null);

    Registration registration = Registration.create("REG-000001", input, PRICE, NOW);

    assertThat(registration.getPayerType()).isEqualTo(PayerType.PRIVATE);
    assertThat(registration.getCompanyName()).isNull();
    assertThat(registration.getCompanyAddress()).isNull();
    assertThat(registration.getCompanyVatId()).isNull();
    assertThat(registration.getWorkshop()).isNull();
    assertThat(registration.getEmail()).isEqualTo("a@c.si");
    assertThat(registration.getLastName()).isEqualTo("Kovač");
  }

  @Test
  void inputNormalisesBlankToNullAndCopiesWorkshops() {
    List<String> workshops = new ArrayList<>(Arrays.asList(" K1 ", null));
    RegistrationInput input =
        new RegistrationInput("  ", "\t", "", " a@c.si ", null, " ", "x", workshops);
    workshops.clear();

    assertThat(input.firstName()).isNull();
    assertThat(input.lastName()).isNull();
    assertThat(input.email()).isNull();
    assertThat(input.payerType()).isEqualTo("a@c.si");
    assertThat(input.companyAddress()).isNull();
    assertThat(input.workshops()).containsExactly("K1", null);
    assertThatThrownBy(() -> input.workshops().add("K2"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void payerTypeMatchesApiValuesExactly() {
    assertThat(PayerType.fromValue("private")).contains(PayerType.PRIVATE);
    assertThat(PayerType.fromValue("company")).contains(PayerType.COMPANY);
    assertThat(PayerType.fromValue("Company")).isEmpty();
    assertThat(PayerType.fromValue(null)).isEmpty();
    assertThat(PayerType.COMPANY.value()).isEqualTo("company");
  }
}
