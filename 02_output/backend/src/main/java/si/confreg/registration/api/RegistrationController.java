package si.confreg.registration.api;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.application.ExportService;
import si.confreg.registration.application.RegistrationQueryService;
import si.confreg.registration.application.RegistrationService;
import si.confreg.registration.domain.Registration;

/** The fixed registration API (architecture.md) and the invoicing export (D-07). */
@RestController
@RequestMapping("/api/registrations")
class RegistrationController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final RegistrationService registrationService;
  private final RegistrationQueryService queryService;
  private final ExportService exportService;

  RegistrationController(
      RegistrationService registrationService,
      RegistrationQueryService queryService,
      ExportService exportService) {
    this.registrationService = registrationService;
    this.queryService = queryService;
    this.exportService = exportService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<RegistrationResponse> register(@RequestBody Map<String, Object> body) {
    Registration registration =
        registrationService.register(RegistrationRequestMapper.toCommand(body));
    return ResponseEntity.created(
            URI.create("/api/registrations/" + registration.getRegistrationNumber()))
        .body(RegistrationResponse.of(registration));
  }

  @GetMapping("/export")
  ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations.xlsx\"")
        .body(exportService.exportWorkbook());
  }

  @GetMapping("/{registrationNumber}")
  RegistrationResponse get(@PathVariable String registrationNumber) {
    return RegistrationResponse.of(queryService.find(registrationNumber));
  }
}
