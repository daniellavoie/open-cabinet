package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The answer to a short test request, proving the model is reachable from the server.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CheckReply {
	String model;
	String text;
	BigDecimal costUsd;
}
