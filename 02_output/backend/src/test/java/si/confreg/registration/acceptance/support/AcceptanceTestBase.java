package si.confreg.registration.acceptance.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import si.confreg.registration.RegistrationApplication;

/**
 * Starts the backend as a black box on a random port against PostgreSQL and the Mailpit mail
 * catcher in containers (images from tech-stack.md). Tests use only HTTP and the mail catcher.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  protected static final Duration MAIL_TIMEOUT = Duration.ofSeconds(30);
  protected static final Duration SETTLE = Duration.ofSeconds(3);

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"))
          .withDatabaseName("registration")
          .withUsername("registration");

  private static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  protected static final SmtpSwitch SMTP;

  static {
    POSTGRES.start();
    MAILPIT.start();
    SMTP = new SmtpSwitch(MAILPIT.getHost(), MAILPIT.getMappedPort(1025));
    SMTP.on();
  }

  @Autowired private Environment environment;

  @DynamicPropertySource
  static void backendSettings(DynamicPropertyRegistry registry) {
    AcceptanceConfig.settings().forEach((name, value) -> registry.add(name, () -> value));
    // Settings named in docs/02_specification.md section 3 ...
    registry.add("APP_DB_URL", POSTGRES::getJdbcUrl);
    registry.add("APP_DB_USER", POSTGRES::getUsername);
    registry.add("POSTGRES_PASSWORD", POSTGRES::getPassword);
    registry.add("APP_SMTP_HOST", () -> "127.0.0.1");
    registry.add("APP_SMTP_PORT", () -> String.valueOf(SMTP.port()));
    registry.add("APP_SMTP_TLS", () -> "false");
    // ... and the framework's own names, so that the bootstrap skeleton starts as well.
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", () -> "127.0.0.1");
    registry.add("spring.mail.port", () -> String.valueOf(SMTP.port()));
  }

  protected ApiClient api() {
    return new ApiClient("http://127.0.0.1:" + environment.getProperty("local.server.port"));
  }

  protected static Mailpit mailpit() {
    return new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
  }

  /** Registers and returns the 201 answer, failing the test with the body otherwise. */
  protected ApiClient.Response registerOk(Map<String, Object> body, Instant now) {
    ApiClient.Response response = api().register(body, now);
    if (response.status() != 201) {
      throw new AssertionError(
          "expected 201 but was " + response.status() + ": " + response.body());
    }
    return response;
  }

  /** Rows of the organizer's invoicing export, header row first, cells as displayed text. */
  protected List<List<String>> exportRows() {
    ApiClient.Response response = api().getAsOrganizer("/api/registrations/export");
    if (response.status() != 200) {
      throw new AssertionError("export answered " + response.status());
    }
    return readSheet(response.raw().body());
  }

  protected static List<List<String>> readSheet(byte[] xlsx) {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheet("Registrations");
      if (sheet == null) {
        throw new AssertionError("sheet 'Registrations' missing");
      }
      DataFormatter formatter = new DataFormatter();
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
          cells.add(row.getCell(i) == null ? "" : formatter.formatCellValue(row.getCell(i)));
        }
        rows.add(cells);
      }
      return rows;
    } catch (IOException e) {
      throw new AssertionError("export is not a readable .xlsx workbook", e);
    }
  }

  /** Number of stored registrations, read through the organizer export. */
  protected int storedCount() {
    return exportRows().size() - 1;
  }
}
