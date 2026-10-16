package dev.daniellavoie.opencabinet.common.module.web;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ErrorResponse {
	String message;
	List<?> details;
}
