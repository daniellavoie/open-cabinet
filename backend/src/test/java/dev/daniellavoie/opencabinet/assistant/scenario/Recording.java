package dev.daniellavoie.opencabinet.assistant.scenario;

import java.math.BigDecimal;
import java.util.List;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;

/**
 * The model's replies in one scenario run, in order, so unit tests can replay them without the
 * model. {@code note} says where the recording came from.
 */
public record Recording(String scenario, String model, String note, List<Reply> replies) {
	@Override
	public String toString() {
		return scenario + " (" + model + ")";
	}

	public record Reply(String text, List<ToolCall> toolCalls, long inputTokens, long outputTokens, BigDecimal costUsd) {
	}
}
