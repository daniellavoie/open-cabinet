package dev.daniellavoie.opencabinet.assistant.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.JsonNode;

/**
 * One tool call in a Design Proposal, with the id of what it created, so replaying it recreates the
 * same ids.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class DesignStep {
	String tool;
	JsonNode arguments;
	String createdId;
}
