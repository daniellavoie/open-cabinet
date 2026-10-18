package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;

public interface ConversationRepository {
	Optional<Conversation> findByProject(UUID projectId, UUID shopId);

	Conversation create(Conversation conversation);

	/**
	 * Marks the Conversation as answering a request, unless it already is.
	 */
	boolean startRunning(UUID conversationId);

	void stopRunning(UUID conversationId);

	/**
	 * Requests cut short by a restart, for every Conversation.
	 */
	List<Conversation> findRunning();

	List<ConversationMessage> findMessages(UUID conversationId);

	ConversationMessage addMessage(ConversationMessage message);

	Optional<DesignProposal> findOpenProposal(UUID projectId, UUID shopId);

	/**
	 * The open Proposal, else the one accepted last.
	 */
	Optional<DesignProposal> findCurrentProposal(UUID projectId, UUID shopId);

	DesignProposal findProposal(UUID proposalId, UUID shopId);

	DesignProposal saveProposal(DesignProposal proposal);
}
