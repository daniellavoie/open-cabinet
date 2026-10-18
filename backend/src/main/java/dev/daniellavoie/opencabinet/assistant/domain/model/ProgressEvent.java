package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.util.List;

import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.JsonNode;

/**
 * What the assistant is doing while it answers: each tool call, whether it was taken or refused,
 * and why.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ProgressEvent {
	int sequence;
	Kind kind;
	String tool;
	JsonNode arguments;
	String createdId;
	List<Violation> violations;
	/**
	 * Why a tool call could not be used, in the model's words.
	 */
	String problem;

	public enum Kind {
		/**
		 * Waiting for the model.
		 */
		THINKING,
		/**
		 * A step was added to the Proposal.
		 */
		STEP,
		/**
		 * A step broke a Construction Rule, or could not be used, and was not taken.
		 */
		REFUSED,
		/**
		 * The assistant noted the Proposal's summary, open questions and assumptions.
		 */
		NOTED;
	}
}
