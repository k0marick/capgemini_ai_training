package com.todomanager.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps domain and validation failures to application/problem+json. */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail notFound() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
	ProblemDetail concurrentModification() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The resource was modified concurrently; reload and retry");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return validation(errors);
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail badRequest(BadRequestException ex) {
		return validation(Map.of(ex.getField(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail typeMismatch(MethodArgumentTypeMismatchException ex) {
		return validation(Map.of(ex.getName(), "invalid value"));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableBody() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
	}

	private static ProblemDetail validation(Map<String, String> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
		problem.setProperty("errors", errors);
		return problem;
	}

}
