package si.confreg.registration.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.confreg.registration.application.ConfirmationFailedException;
import si.confreg.registration.application.RegistrationRejectedException;

/**
 * Maps failures to the documented error bodies, without internal details (SB-07, ES-07). Logs
 * contain no request data or personal data (SR-01).
 */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  ResponseEntity<ErrorBody> rejected(RegistrationRejectedException e) {
    return ResponseEntity.badRequest().body(new ErrorBody("validation_failed", e.invalidFields()));
  }

  @ExceptionHandler({MalformedRequestException.class, HttpMessageNotReadableException.class})
  ResponseEntity<ErrorBody> malformed() {
    return ResponseEntity.badRequest().body(ErrorBody.of("malformed_request"));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorBody> unsupportedMediaType() {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ErrorBody.of("unsupported_media_type"));
  }

  @ExceptionHandler({
    NotFoundException.class,
    NoResourceFoundException.class,
    NoHandlerFoundException.class,
    HttpRequestMethodNotSupportedException.class,
    HttpMediaTypeNotAcceptableException.class
  })
  ResponseEntity<ErrorBody> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody.of("not_found"));
  }

  @ExceptionHandler(ConfirmationFailedException.class)
  ResponseEntity<ErrorBody> confirmationFailed(ConfirmationFailedException e) {
    LOG.warn(
        "Confirmation e-mail not accepted ({}); registration rolled back",
        e.getCause() == null ? "unknown" : e.getCause().getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorBody.of("registration_unavailable"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception e) {
    LOG.error("Unexpected failure ({})", e.getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorBody.of("internal_error"));
  }
}
