package si.confreg.registration.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** The documented error body (docs/02_contracts/registration-api.openapi.yaml, Error). */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ErrorBody(String error, List<String> fields) {

  static ErrorBody of(String error) {
    return new ErrorBody(error, null);
  }
}
