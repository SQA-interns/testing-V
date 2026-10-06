package si.confreg.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Checks exactly the architecture declared in docs/02_specification.md section 2 (AR-02, AR-03,
 * AR-05, AR-07; DoD-04). Rule names A1 to A7 match the specification.
 */
@AnalyzeClasses(
    packages = "si.confreg.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.confreg.registration";
  private static final String DOMAIN = ROOT + ".domain..";
  private static final String APPLICATION = ROOT + ".application..";
  private static final String WEB = ROOT + ".web..";
  private static final String PERSISTENCE = ROOT + ".persistence..";
  private static final String MAIL = ROOT + ".mail..";
  private static final String CONFIG = ROOT + ".config..";

  @ArchTest
  static final ArchRule a1DomainIsIndependent =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              APPLICATION,
              WEB,
              PERSISTENCE,
              MAIL,
              CONFIG,
              "org.springframework..",
              "jakarta.persistence..",
              "jakarta.servlet..")
          .as("A1 domain depends on no other layer and no framework");

  @ArchTest
  static final ArchRule a2ApplicationDependsOnlyOnDomain =
      noClasses()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              WEB,
              PERSISTENCE,
              MAIL,
              CONFIG,
              "jakarta.persistence..",
              "jakarta.servlet..",
              "org.springframework.web..")
          .as("A2 application depends only on domain");

  @ArchTest
  static final ArchRule a3WebDoesNotUseOtherAdapters =
      noClasses()
          .that()
          .resideInAPackage(WEB)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(PERSISTENCE, MAIL)
          .as("A3 web does not depend on persistence or mail");

  @ArchTest
  static final ArchRule a3PersistenceDoesNotUseOtherAdapters =
      noClasses()
          .that()
          .resideInAPackage(PERSISTENCE)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(WEB, MAIL)
          .as("A3 persistence does not depend on web or mail");

  @ArchTest
  static final ArchRule a3MailDoesNotUseOtherAdapters =
      noClasses()
          .that()
          .resideInAPackage(MAIL)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(WEB, PERSISTENCE)
          .as("A3 mail does not depend on web or persistence");

  @ArchTest
  static final ArchRule a4OnlyConfigUsesConfig =
      noClasses()
          .that()
          .resideOutsideOfPackage(CONFIG)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(CONFIG)
          .as("A4 no package other than config depends on config");

  @ArchTest
  static final ArchRule a5NoCycles =
      slices()
          .matching(ROOT + ".(*)..")
          .should()
          .beFreeOfCycles()
          .as("A5 no cycles between packages (AR-03)");

  @ArchTest
  static final ArchRule a6OnlyPersistenceUsesJpa =
      noClasses()
          .that()
          .resideOutsideOfPackage(PERSISTENCE)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
          .as("A6 only persistence uses JPA and Spring Data");

  @ArchTest
  static final ArchRule a6OnlyMailUsesMail =
      noClasses()
          .that()
          .resideOutsideOfPackages(MAIL, CONFIG)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..")
          .as("A6 only mail sends e-mail (AR-07); config only wires the sender");

  @ArchTest
  static final ArchRule a7OneClockComponent =
      noClasses()
          .that()
          .resideOutsideOfPackage(CONFIG)
          .and()
          .doNotHaveFullyQualifiedName(ROOT + ".web.RequestTimeSource")
          .should()
          .callMethod(Instant.class, "now")
          .orShould()
          .callMethod(LocalDate.class, "now")
          .orShould()
          .callMethod(LocalDateTime.class, "now")
          .orShould()
          .callMethod(ZonedDateTime.class, "now")
          .orShould()
          .callMethod(System.class, "currentTimeMillis")
          .orShould()
          .callMethod(Clock.class, "systemUTC")
          .orShould()
          .callMethod(Clock.class, "systemDefaultZone")
          .as("A7 only the clock component reads the system time (AR-05)");
}
