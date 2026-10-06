package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WorkshopCatalogueTest {

  @Test
  void parsesPairsInOrderAndTrims() {
    WorkshopCatalogue catalogue = WorkshopCatalogue.parse(" A = First ; B=Second;");

    assertThat(catalogue.workshops())
        .containsExactly(new Workshop("A", "First"), new Workshop("B", "Second"));
    assertThat(catalogue.find("B")).contains(new Workshop("B", "Second"));
    assertThat(catalogue.find("C")).isEmpty();
  }

  @Test
  void titleMayContainEqualsSign() {
    assertThat(WorkshopCatalogue.parse("A=x=y").find("A")).contains(new Workshop("A", "x=y"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ; ", "A", "=Title", "A=", "A= "})
  void rejectsInvalidDefinitions(String definition) {
    assertThatThrownBy(() -> WorkshopCatalogue.parse(definition))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void listIsImmutable() {
    WorkshopCatalogue catalogue = WorkshopCatalogue.parse("A=First");
    assertThatThrownBy(() -> catalogue.workshops().add(new Workshop("B", "x")))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
