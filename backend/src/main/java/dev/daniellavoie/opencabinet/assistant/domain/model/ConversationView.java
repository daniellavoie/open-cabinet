package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Project's Conversation as a Member sees it: the messages, what the assistant is doing, and
 * the open Design Proposal, or the last one accepted.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ConversationView {
	UUID conversationId;
	boolean running;
	List<ConversationMessage> messages;
	List<ProgressEvent> progress;
	DesignProposal proposal;
}
