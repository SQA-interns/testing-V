package si.confreg.registration.api;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.application.BusinessSettings;

/** Configured workshops for the registration form (D-28, AR-04). */
@RestController
class WorkshopController {

  record WorkshopResponse(String id, String title) {}

  private final BusinessSettings settings;

  WorkshopController(BusinessSettings settings) {
    this.settings = settings;
  }

  @GetMapping(path = "/api/workshops", produces = MediaType.APPLICATION_JSON_VALUE)
  List<WorkshopResponse> workshops() {
    return settings.workshops().workshops().stream()
        .map(w -> new WorkshopResponse(w.id(), w.title()))
        .toList();
  }
}
