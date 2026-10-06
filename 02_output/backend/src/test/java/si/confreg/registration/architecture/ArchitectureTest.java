package si.confreg.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The architecture declared in docs/02_specification.md section 2 (AR-02, AR-03, AR-05..07). */
@AnalyzeClasses(
    packages = "si.confreg.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.confreg.registration";

  /** ARCH-1 (AR-02): the layer table. */
  @ArchTest
  static final ArchRule layers =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("api")
          .definedBy(ROOT + ".api..")
          .layer("security")
          .definedBy(ROOT + ".security..")
          .layer("application")
          .definedBy(ROOT + ".application..")
          .layer("domain")
          .definedBy(ROOT + ".domain..")
          .layer("persistence")
          .definedBy(ROOT + ".persistence..")
          .layer("mail")
          .definedBy(ROOT + ".mail..")
          .layer("config")
          .definedBy(ROOT + ".config..")
          .whereLayer("api")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("security")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("application")
          .mayOnlyBeAccessedByLayers("api")
          .whereLayer("persistence")
          .mayOnlyBeAccessedByLayers("application")
          .whereLayer("mail")
          .mayOnlyBeAccessedByLayers("application")
          .whereLayer("domain")
          .mayOnlyBeAccessedByLayers("api", "application", "persistence", "mail")
          .whereLayer("config")
          .mayOnlyBeAccessedByLayers(
              "api", "security", "application", "domain", "persistence", "mail");

  /** ARCH-2 (AR-03): no package cycles. */
  @ArchTest
  static final ArchRule noCycles = slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();

  /** ARCH-3 (AR-07): only the mail component sends e-mail. */
  @ArchTest
  static final ArchRule onlyMailSendsEmail =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".mail..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..");

  /** ARCH-4 (AR-05): only the conference clock reads the current time. */
  @ArchTest
  static final ArchRule onlyConferenceClockReadsTime =
      noClasses()
          .that()
          .doNotHaveFullyQualifiedName(ROOT + ".config.ConferenceClock")
          .should()
          .callMethod(java.time.Instant.class, "now")
          .orShould()
          .callMethod(java.time.LocalDate.class, "now")
          .orShould()
          .callMethod(java.time.LocalDateTime.class, "now")
          .orShould()
          .callMethod(java.time.ZonedDateTime.class, "now")
          .orShould()
          .callMethod(java.time.OffsetDateTime.class, "now")
          .orShould()
          .callMethod(System.class, "currentTimeMillis")
          .orShould()
          .callMethod(java.time.Clock.class, "systemUTC")
          .orShould()
          .callMethod(java.time.Clock.class, "systemDefaultZone");

  /** ARCH-5: the domain is free of web, servlet and persistence frameworks. */
  @ArchTest
  static final ArchRule domainIsFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework.web..", "jakarta.persistence..", "jakarta.servlet..");

  /** ARCH-6 (AR-06): no native DDL from code. */
  @ArchTest
  static final ArchRule noSchemaChangesFromCode =
      noClasses()
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("jakarta.persistence.EntityManager");

  /** ARCH-6 (AR-06): Hibernate only validates the schema; Flyway changes it. */
  @Test
  void schemaIsValidatedNotGenerated() throws IOException {
    try (InputStream yaml = getClass().getResourceAsStream("/application.yml")) {
      String content = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);
      org.assertj.core.api.Assertions.assertThat(content).contains("ddl-auto: validate");
    }
  }
}
