package com.threadly.common.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import com.threadly.auth.refresh.InvalidRefreshTokenException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC 7807 problem responses so every error the API returns has the
 * same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail onValidationFailure(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			// Keep the first message per field; repeated messages add noise for clients.
			errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setType(URI.create("https://threadly.dev/problems/validation-failed"));
		problem.setTitle("Validation failed");
		problem.setDetail("One or more fields are invalid.");
		problem.setProperty("errors", errors);
		return problem;
	}

	/**
	 * Every authentication failure gets the same body. Distinguishing "no such account" from "wrong
	 * password" or "account disabled" would let anyone probe which handles are registered.
	 */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail onAuthenticationFailure(AuthenticationException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setType(URI.create("https://threadly.dev/problems/invalid-credentials"));
		problem.setTitle("Invalid credentials");
		problem.setDetail("The identifier or password is incorrect.");
		return problem;
	}

	/**
	 * Narrower than the handler above: a rejected refresh is about the session, not the password,
	 * and the client's correct reaction is to send the user back to the login screen.
	 */
	@ExceptionHandler(InvalidRefreshTokenException.class)
	ProblemDetail onInvalidRefreshToken(InvalidRefreshTokenException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setType(URI.create("https://threadly.dev/problems/invalid-refresh-token"));
		problem.setTitle("Session expired");
		problem.setDetail("This session is no longer valid. Sign in again.");
		return problem;
	}

	/**
	 * Raised by {@code @Validated} on request parameters, which does not go through the binding
	 * result the handler above inspects.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail onParameterConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(violation -> {
			String path = violation.getPropertyPath().toString();
			errors.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
		});

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setType(URI.create("https://threadly.dev/problems/validation-failed"));
		problem.setTitle("Validation failed");
		problem.setDetail("One or more request parameters are invalid.");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail onBadRequest(BadRequestException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setType(URI.create("https://threadly.dev/problems/bad-request"));
		problem.setTitle("Bad request");
		problem.setDetail(exception.getMessage());
		return problem;
	}

	@ExceptionHandler(InvalidCursorException.class)
	ProblemDetail onInvalidCursor(InvalidCursorException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setType(URI.create("https://threadly.dev/problems/invalid-cursor"));
		problem.setTitle("Invalid cursor");
		problem.setDetail(exception.getMessage());
		return problem;
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail onAccessDenied(AccessDeniedException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setType(URI.create("https://threadly.dev/problems/forbidden"));
		problem.setTitle("Forbidden");
		problem.setDetail(exception.getMessage());
		return problem;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail onNotFound(ResourceNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setType(URI.create("https://threadly.dev/problems/not-found"));
		problem.setTitle("Not found");
		problem.setDetail(exception.getMessage());
		return problem;
	}

	@ExceptionHandler(DuplicateResourceException.class)
	ProblemDetail onDuplicate(DuplicateResourceException exception) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setType(URI.create("https://threadly.dev/problems/duplicate-resource"));
		problem.setTitle("Resource already exists");
		problem.setDetail(exception.getMessage());
		problem.setProperty("field", exception.getField());
		return problem;
	}
}
