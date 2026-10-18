package dev.daniellavoie.opencabinet.assistant.domain.model;

public enum ProposalStatus {
	/**
	 * Changes through the Conversation until a Member accepts or discards it.
	 */
	OPEN,
	ACCEPTED,
	DISCARDED;
}
