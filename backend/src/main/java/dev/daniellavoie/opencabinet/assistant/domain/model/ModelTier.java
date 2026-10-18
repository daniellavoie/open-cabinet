package dev.daniellavoie.opencabinet.assistant.domain.model;

/**
 * Which model a request needs; each is set in configuration.
 */
public enum ModelTier {
	/**
	 * Builds Design Proposals.
	 */
	BUILDER,
	/**
	 * Sorts the Inbox: summaries and matching an email to a Client and Project.
	 */
	SORTER;
}
