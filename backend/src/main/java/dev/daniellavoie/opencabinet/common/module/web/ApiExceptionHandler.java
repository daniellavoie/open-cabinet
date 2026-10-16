package dev.daniellavoie.opencabinet.common.module.web;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.daniellavoie.opencabinet.project.domain.model.ConstructionRuleException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> conflict(IllegalStateException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), List.of());
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> notFound(NoSuchElementException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), List.of());
	}

	@ExceptionHandler(ConstructionRuleException.class)
	public ResponseEntity<ErrorResponse> constructionRules(ConstructionRuleException exception) {
		return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception.getViolations());
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, List<?> details) {
		return ResponseEntity.status(status)
				.body(ErrorResponse.builder()
						.message(message)
						.details(details)
						.build());
	}
}
