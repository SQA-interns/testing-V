package si.confreg.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import si.confreg.registration.domain.FieldError;
import si.confreg.registration.service.InvalidRegistrationException;
import si.confreg.registration.service.RegistrationNotCompletedException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void validationProblemListsFieldsWithoutValues() {
    ProblemDetail problem =
        handler.invalid(
            new InvalidRegistrationException(
                List.of(new FieldError("email", "must be a valid e-mail address"))));

    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getTitle()).isEqualTo("Bad Request");
    assertThat(problem.getProperties())
        .containsEntry(
            "errors",
            List.of(Map.of("field", "email", "message", "must be a valid e-mail address")));
  }

  @Test
  void mailFailureIsServiceUnavailableWithRetryHint() {
    ProblemDetail problem =
        handler.notCompleted(new RegistrationNotCompletedException(new IllegalStateException()));

    assertThat(problem.getStatus()).isEqualTo(503);
    assertThat(problem.getDetail()).contains("try again");
  }

  @Test
  void statusesForStandardFailures() {
    assertThat(handler.notFound(new RegistrationNotFoundException()).getStatus()).isEqualTo(404);
    assertThat(
            handler
                .methodNotAllowed(new HttpRequestMethodNotSupportedException(HttpMethod.PUT.name()))
                .getStatus())
        .isEqualTo(405);
  }

  @Test
  void unexpectedErrorsRevealNoInternals() {
    ProblemDetail problem = handler.unexpected(new IllegalStateException("SELECT secret FROM x"));

    assertThat(problem.getStatus()).isEqualTo(500);
    assertThat(problem.getDetail()).doesNotContain("SELECT").doesNotContain("IllegalState");
  }

  @Test
  void otherClientErrorsKeepTheirStatus() {
    ProblemDetail problem = handler.unexpected(new MissingRequestHeaderException("X-Any", null));

    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getDetail()).doesNotContain("X-Any");
  }
}
