package si.confreg.registration.acceptance;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** Architecture declared in docs/02_specification.md, section 2 (AR-02, AR-03, AR-05, AR-07). */
class ArchitectureAcceptanceTest {

  private static final String ROOT = "si.confreg.registration";
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  private static String pkg(String name) {
    return ROOT + "." + name + "..";
  }

  @Test
  void AR_02_rule1_domainDependsOnNoOtherProjectPackageAndNoFramework() {
    noClasses()
        .that()
        .resideInAPackage(pkg("domain"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            pkg("application"),
            pkg("api"),
            pkg("persistence"),
            pkg("mail"),
            pkg("clock"),
            pkg("security"),
            pkg("config"),
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.mail..")
        .check(CLASSES);
  }

  @Test
  void AR_02_rule2_applicationDependsOnlyOnDomainAndNoPersistenceOrMail() {
    noClasses()
        .that()
        .resideInAPackage(pkg("application"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            pkg("api"),
            pkg("persistence"),
            pkg("mail"),
            pkg("clock"),
            pkg("security"),
            pkg("config"),
            "jakarta.persistence..",
            "jakarta.mail..")
        .check(CLASSES);
  }

  @Test
  void AR_02_rule3_apiDoesNotDependOnAdapters() {
    noClasses()
        .that()
        .resideInAPackage(pkg("api"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(pkg("persistence"), pkg("mail"), pkg("clock"))
        .check(CLASSES);
  }

  @Test
  void AR_02_rule4_adaptersDoNotDependOnApiOrEachOther() {
    noClasses()
        .that()
        .resideInAPackage(pkg("persistence"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(pkg("api"), pkg("mail"), pkg("clock"))
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage(pkg("mail"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(pkg("api"), pkg("persistence"), pkg("clock"))
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage(pkg("clock"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(pkg("api"), pkg("persistence"), pkg("mail"))
        .check(CLASSES);
  }

  @Test
  void AR_02_rule5_onlyPersistenceUsesJpa() {
    noClasses()
        .that()
        .resideOutsideOfPackage(pkg("persistence"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
        .check(CLASSES);
  }

  @Test
  void AR_07_rule5_onlyMailComponentSendsEmail() {
    noClasses()
        .that()
        .resideOutsideOfPackage(pkg("mail"))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..")
        .check(CLASSES);
  }

  @Test
  void AR_05_rule6_onlyClockComponentReadsTheCurrentTime() {
    noClasses()
        .that()
        .resideOutsideOfPackage(pkg("clock"))
        .should()
        .callMethod(Instant.class, "now")
        .orShould()
        .callMethod(LocalDate.class, "now")
        .orShould()
        .callMethod(LocalDateTime.class, "now")
        .orShould()
        .callMethod(ZonedDateTime.class, "now")
        .orShould()
        .callMethod(OffsetDateTime.class, "now")
        .orShould()
        .callMethod(Clock.class, "systemUTC")
        .orShould()
        .callMethod(Clock.class, "systemDefaultZone")
        .check(CLASSES);
  }

  @Test
  void AR_03_rule7_noCyclesBetweenPackages() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
  }
}
