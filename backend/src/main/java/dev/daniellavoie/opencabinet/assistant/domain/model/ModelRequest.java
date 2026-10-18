package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * One request to the model: instructions, then the conversation so far, oldest first.
 */
@Value
@Builder(toBuilder = true)
public class ModelRequest {
	ModelTier tier;
	/**
	 * What the request is for, recorded with its cost.
	 */
	String purpose;
	String instructions;
	List<ModelMessage> messages;
	int maxTokens;

	public record ModelMessage(Speaker speaker, String text) {
	}

	public enum Speaker {
		MEMBER, ASSISTANT;
	}
}
