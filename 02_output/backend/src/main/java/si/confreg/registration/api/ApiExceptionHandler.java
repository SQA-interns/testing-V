package si.confreg.registration.api;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.confreg.registration.application.DuplicateRegistrationException;
import si.confreg.registration.application.FieldError;
import si.confreg.registration.application.PayloadTooLargeException;
import si.confreg.registration.application.RegistrationNotFoundException;
import si.confreg.registration.application.ValidationFailedException;

/**
 * Maps every error to {@code application/problem+json} with fixed texts (ES-07, SB-07). Exception
 * messages are never returned or logged, because they may contain submitted values (SR-01).
 */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;
  private static final int BODY_LIMIT_BYTES = 16 * 1024;

  record Problem(String type, String title, int status, String detail, List<FieldError> errors) {}

  private static ResponseEntity<Problem> problem(
      HttpStatus status, String title, String detail, List<FieldError> errors) {
    return ResponseEntity.status(status)
        .contentType(PROBLEM)
        .body(new Problem("about:blank", title, status.value(), detail, errors));
  }

  @ExceptionHandler(ValidationFailedException.class)
  ResponseEntity<Problem> validation(ValidationFailedException e) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid registration",
        "Some fields are missing or invalid.",
        e.errors());
  }

  @ExceptionHandler(DuplicateRegistrationException.class)
  ResponseEntity<Problem> duplicate() {
    return problem(
        HttpStatus.CONFLICT,
        "Already registered",
        "A registration with this e-mail address already exists.",
        null);
  }

  @ExceptionHandler(RegistrationNotFoundException.class)
  ResponseEntity<Problem> notFound() {
    return problem(HttpStatus.NOT_FOUND, "Not found", "No such registration.", null);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Problem> unreadable(HttpMessageNotReadableException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof PayloadTooLargeException) {
        return tooLarge();
      }
    }
    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "The request body is not a valid JSON object.",
        null);
  }

  @ExceptionHandler(PayloadTooLargeException.class)
  ResponseEntity<Problem> tooLarge() {
    return problem(
        HttpStatus.CONTENT_TOO_LARGE,
        "Request too large",
        "The request body exceeds " + BODY_LIMIT_BYTES + " bytes.",
        null);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Problem> mediaType() {
    return problem(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Unsupported media type",
        "Send the request as application/json.",
        null);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<Problem> badParameter() {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", "A parameter is invalid.", null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Problem> method() {
    return problem(
        HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "Method not allowed.", null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Problem> noResource() {
    return problem(HttpStatus.NOT_FOUND, "Not found", "Not found.", null);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Problem> unexpected(Exception e) {
    LOG.error("Unexpected error: {}", e.getClass().getName());
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, "Server error", "An unexpected error occurred.", null);
  }
}
