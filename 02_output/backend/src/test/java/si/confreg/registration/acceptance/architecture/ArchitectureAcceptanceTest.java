package si.confreg.registration.acceptance.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/** AR-02 … AR-07 as declared in docs/02_specification.md section 2 (ARCH-1 … ARCH-5). */
@AnalyzeClasses(
    packages = "si.confreg.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureAcceptanceTest {

  private static final String ROOT = "si.confreg.registration";

  @ArchTest
  static final ArchRule arch1_ar02_layersAsDeclared =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("api")
          .definedBy(ROOT + ".api..")
          .layer("application")
          .definedBy(ROOT + ".application..")
          .layer("domain")
          .definedBy(ROOT + ".domain..")
          .layer("infrastructure")
          .definedBy(ROOT + ".infrastructure..")
          .layer("config")
          .definedBy(ROOT + ".config..")
          .whereLayer("api")
          .mayOnlyBeAccessedByLayers("config")
          .whereLayer("infrastructure")
          .mayOnlyBeAccessedByLayers("config")
          .whereLayer("application")
          .mayOnlyBeAccessedByLayers("api", "infrastructure", "config")
          .whereLayer("domain")
          .mayOnlyBeAccessedByLayers("api", "application", "infrastructure", "config")
          .whereLayer("config")
          .mayNotBeAccessedByAnyLayer();

  @ArchTest
  static final ArchRule arch2_ar03_noPackageCycles =
      slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule arch3_ar07_onlyMailComponentSendsMail =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".infrastructure.mail..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..");

  @ArchTest
  static final ArchRule arch4_ar05_onlyClockComponentReadsCurrentTime =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".infrastructure.clock..")
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
          .callMethod(Clock.class, "systemDefaultZone");

  @ArchTest
  static final ArchRule arch5_ar01_onlyApiLayerDefinesWebEndpoints =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework.web.bind.annotation..");
}
