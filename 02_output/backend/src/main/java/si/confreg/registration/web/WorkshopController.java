package si.confreg.registration.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.domain.WorkshopCatalog;

/** Public list of the configured workshops for the form (AC-001-11, D-10, spec 5.3). */
@RestController
public class WorkshopController {

  /** Workshop as JSON. */
  record WorkshopResponse(String id, String title) {}

  private final WorkshopCatalog workshops;

  public WorkshopController(WorkshopCatalog workshops) {
    this.workshops = workshops;
  }

  @GetMapping(path = "/api/workshops")
  public ResponseEntity<List<WorkshopResponse>> list() {
    List<WorkshopResponse> body =
        workshops.all().stream().map(w -> new WorkshopResponse(w.id(), w.title())).toList();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
  }
}
