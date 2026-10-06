package si.confreg.registration.api;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import si.confreg.registration.domain.ValidationFailedException;

/**
 * Maps errors to RFC 9457 problem details without internal details or personal data (SB-07, SR-01).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ValidationFailedException.class)
  ProblemDetail validationFailed(ValidationFailedException exception) {
    ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed");
    List<Map<String, String>> errors =
        exception.errors().stream()
            .map(error -> Map.of("field", error.field(), "message", error.message()))
            .toList();
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(NotAJsonObjectException.class)
  ProblemDetail notAnObject() {
    return problem(HttpStatus.BAD_REQUEST, "Request body must be a JSON object");
  }

  @ExceptionHandler(RegistrationNotFoundException.class)
  ProblemDetail notFound() {
    return problem(HttpStatus.NOT_FOUND, "Registration not found");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception exception) {
    LOG.error("Unexpected error: {}", exception.getClass().getName());
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
  }

  private static ProblemDetail problem(HttpStatus status, String title) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setTitle(title);
    return problem;
  }
}
