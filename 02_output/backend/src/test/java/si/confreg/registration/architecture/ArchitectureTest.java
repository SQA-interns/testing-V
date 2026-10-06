package si.confreg.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.repository.Repository;
import org.springframework.mail.javamail.JavaMailSender;

/** Architecture rules ARCH-1 to ARCH-6 of docs/02_specification.md section 5 (AR-02 to AR-07). */
class ArchitectureTest {

  private static final String ROOT = "si.confreg.registration";
  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @Test
  void arch1_layersAccessOnlyDeclaredLayers() {
    ArchRule rule =
        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(ROOT + "..")
            .layer("api")
            .definedBy(ROOT + ".api..")
            .layer("service")
            .definedBy(ROOT + ".service..")
            .layer("domain")
            .definedBy(ROOT + ".domain..")
            .layer("persistence")
            .definedBy(ROOT + ".persistence..")
            .layer("mail")
            .definedBy(ROOT + ".mail..")
            .layer("time")
            .definedBy(ROOT + ".time..")
            .layer("security")
            .definedBy(ROOT + ".security..")
            .layer("config")
            .definedBy(ROOT + ".config..")
            .whereLayer("api")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("security")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("service")
            .mayOnlyBeAccessedByLayers("api")
            .whereLayer("persistence")
            .mayOnlyBeAccessedByLayers("service")
            .whereLayer("mail")
            .mayOnlyBeAccessedByLayers("service")
            .whereLayer("time")
            .mayOnlyBeAccessedByLayers("service")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("api", "service", "persistence", "mail")
            .whereLayer("config")
            .mayOnlyBeAccessedByLayers("service", "mail", "time", "security");
    rule.check(classes);
  }

  @Test
  void arch1_domainAndConfigDependOnNoOtherApplicationPackage() {
    noClasses()
        .that()
        .resideInAnyPackage(ROOT + ".domain..", ROOT + ".config..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".api..",
            ROOT + ".service..",
            ROOT + ".persistence..",
            ROOT + ".mail..",
            ROOT + ".time..",
            ROOT + ".security..")
        .check(classes);
  }

  @Test
  void arch2_noPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
  }

  @Test
  void arch3_onlyTimeReadsTheSystemClock() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".time..")
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
        .orShould()
        .callMethod(System.class, "currentTimeMillis")
        .check(classes);
  }

  @Test
  void arch4_onlyMailUsesMailApis() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".mail..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.mail..", "org.springframework.mail..")
        .orShould()
        .dependOnClassesThat()
        .areAssignableTo(JavaMailSender.class)
        .check(classes);
  }

  @Test
  void arch5_onlyConfigReadsConfiguration() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".config..")
        .should()
        .beAnnotatedWith(ConfigurationProperties.class)
        .orShould()
        .dependOnClassesThat()
        .areAssignableTo(Value.class)
        .check(classes);
  }

  @Test
  void arch6_onlyPersistenceDeclaresRepositories() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".persistence..")
        .should()
        .beAssignableTo(Repository.class)
        .check(classes);
  }
}
