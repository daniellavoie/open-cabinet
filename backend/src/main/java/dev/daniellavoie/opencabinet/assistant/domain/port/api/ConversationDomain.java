package dev.daniellavoie.opencabinet.assistant.domain.port.api;

import java.security.Principal;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.AcceptResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;

/**
 * A Project's Conversation with the Design Assistant and its Design Proposals (ADR 0004). Chatting
 * needs no editing lock; accepting takes it.
 */
public interface ConversationDomain {
	/**
	 * The Conversation, with the progress events after {@code afterProgress}.
	 */
	ConversationView view(UUID projectId, int afterProgress, Principal principal);

	/**
	 * Adds the Member's message and starts the assistant's answer in the background; refused while
	 * it still answers the last one.
	 */
	ConversationView send(UUID projectId, SendMessageRequest request, Principal principal);

	/**
	 * Replays the open Proposal's steps onto the Project's current design under the editing lock,
	 * stopping at the first step that breaks a Construction Rule, and saves the Project.
	 */
	AcceptResult accept(UUID projectId, UUID proposalId, Principal principal);

	/**
	 * Restores the design from before accepting, if nobody has saved the Project since; the Proposal
	 * is open again.
	 */
	AcceptResult undoAccept(UUID projectId, UUID proposalId, Principal principal);

	DesignProposal discard(UUID projectId, UUID proposalId, Principal principal);

	/**
	 * Closes requests a restart cut short.
	 */
	int interruptRunning();
}
