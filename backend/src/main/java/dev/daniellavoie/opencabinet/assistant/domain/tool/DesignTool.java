package dev.daniellavoie.opencabinet.assistant.domain.tool;

import java.util.Set;

import tools.jackson.databind.JsonNode;

import dev.daniellavoie.opencabinet.project.domain.model.Design;

/**
 * A tool the Design Assistant calls to edit a Design Proposal: one of the editor's own actions
 * (docs/adr/0004-design-assistant-proposes-member-accepts.md). Applying it again with the same
 * arguments and ids replays it onto another design.
 */
public interface DesignTool {
	String name();

	String description();

	/**
	 * JSON Schema of the arguments.
	 */
	String inputSchema();

	/**
	 * The editor actions ({@code design-edits.ts}) this tool performs.
	 */
	Set<String> editorActions();

	/**
	 * Applies the tool; rejects arguments it cannot use with an {@link IllegalArgumentException}.
	 */
	Outcome apply(Design design, JsonNode arguments, DesignToolContext context);

	/**
	 * The edited design and the id of what the tool created, if anything.
	 */
	record Outcome(Design design, String createdId) {
	}
}
