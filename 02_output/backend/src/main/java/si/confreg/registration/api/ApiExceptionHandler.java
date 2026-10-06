package si.confreg.registration.api;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.confreg.registration.service.InvalidRegistrationException;
import si.confreg.registration.service.RegistrationNotCompletedException;

/**
 * Maps failures to RFC 9457 problem details without internal details or submitted values (SB-07,
 * ES-07, docs/02_specification.md 6.1).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(InvalidRegistrationException.class)
  ProblemDetail invalid(InvalidRegistrationException e) {
    ProblemDetail problem =
        problem(HttpStatus.BAD_REQUEST, "The registration contains invalid fields.");
    List<Map<String, String>> errors =
        e.errors().stream()
            .map(error -> Map.of("field", error.field(), "message", error.message()))
            .toList();
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpMessageNotReadableException e) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "The request body is not valid JSON for a registration or contains unknown fields.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ProblemDetail unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
    return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request body must be JSON.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return problem(HttpStatus.METHOD_NOT_ALLOWED, "This method is not supported here.");
  }

  @ExceptionHandler({RegistrationNotFoundException.class, NoResourceFoundException.class})
  ProblemDetail notFound(Exception e) {
    return problem(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(RegistrationNotCompletedException.class)
  ProblemDetail notCompleted(RegistrationNotCompletedException e) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "The registration could not be completed. Please try again later.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e) {
    if (e instanceof ErrorResponse response && response.getStatusCode().is4xxClientError()) {
      HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
      return problem(status, "The request cannot be processed.");
    }
    LOG.error("Unexpected error: {}", e.getClass().getName());
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    return problem;
  }
}
