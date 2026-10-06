package si.confreg.registration.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationRepository;

/**
 * Invoicing export for the accounting system (AC-001-05, AR-08, D-07). Text is written as string
 * cells only, so no value is ever interpreted as a formula.
 */
@Service
public class ExportService {

  static final List<String> COLUMNS =
      List.of(
          "Registration number",
          "Registered at",
          "First name",
          "Last name",
          "E-mail",
          "Payer type",
          "Company name",
          "Company address",
          "Company VAT ID",
          "Workshop",
          "Student",
          "Net fee",
          "VAT",
          "Gross fee");

  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final RegistrationRepository repository;
  private final AppProperties properties;

  public ExportService(RegistrationRepository repository, AppProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public byte[] exportWorkbook() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      CellStyle amountStyle = workbook.createCellStyle();
      amountStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
      Row header = sheet.createRow(0);
      for (int i = 0; i < COLUMNS.size(); i++) {
        header.createCell(i).setCellValue(COLUMNS.get(i));
      }
      int rowIndex = 1;
      for (Registration registration : repository.findAllByOrderByRegistrationNumberAsc()) {
        writeRow(sheet.createRow(rowIndex++), registration, amountStyle);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("export failed", e);
    }
  }

  private void writeRow(Row row, Registration registration, CellStyle amountStyle) {
    int column = 0;
    text(row, column++, registration.getRegistrationNumber());
    text(
        row,
        column++,
        TIMESTAMP.format(registration.getRegisteredAt().atZone(properties.conferenceTz())));
    text(row, column++, registration.getFirstName());
    text(row, column++, registration.getLastName());
    text(row, column++, registration.getEmail());
    text(row, column++, registration.getPayerType().apiValue());
    text(row, column++, registration.getCompanyName());
    text(row, column++, registration.getCompanyAddress());
    text(row, column++, registration.getCompanyVatId());
    text(row, column++, registration.getWorkshop());
    text(row, column++, registration.isStudent() ? "yes" : "no");
    amount(row, column++, registration.getNetFee(), amountStyle);
    amount(row, column++, registration.getVat(), amountStyle);
    amount(row, column, registration.getGrossFee(), amountStyle);
  }

  private static void text(Row row, int column, String value) {
    Cell cell = row.createCell(column);
    if (value != null) {
      cell.setCellValue(value);
    }
  }

  private static void amount(Row row, int column, BigDecimal value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value.doubleValue());
    cell.setCellStyle(style);
  }
}
