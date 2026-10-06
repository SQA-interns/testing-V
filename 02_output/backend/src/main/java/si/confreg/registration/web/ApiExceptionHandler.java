package si.confreg.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps errors to problem responses without internal details (SB-07, ES-07, spec 5.1). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Object> unreadable(HttpServletRequest request) {
    if (RequestBodyLimitFilter.exceeded(request)) {
      return Problems.response(HttpStatus.CONTENT_TOO_LARGE);
    }
    return Problems.response(HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<Object> missingHeader() {
    return Problems.response(HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Object> unsupportedMediaType() {
    return Problems.response(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  ResponseEntity<Object> notAcceptable() {
    return Problems.response(HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Object> methodNotAllowed() {
    return Problems.response(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Object> notFound() {
    return Problems.response(HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception error) {
    LOG.error("unexpected error: {}", error.getClass().getName());
    return Problems.response(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
