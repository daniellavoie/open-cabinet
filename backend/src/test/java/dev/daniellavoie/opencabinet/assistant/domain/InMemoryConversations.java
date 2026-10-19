package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.ConversationRepository;

/**
 * Conversations, messages and Proposals kept in memory.
 */
public final class InMemoryConversations implements ConversationRepository {
	private final Map<UUID, Conversation> conversations = new HashMap<>();
	private final List<ConversationMessage> messages = new ArrayList<>();
	private final Map<UUID, DesignProposal> proposals = new HashMap<>();

	@Override
	public Optional<Conversation> findByProject(UUID projectId, UUID shopId) {
		return conversations.values().stream()
				.filter(conversation -> conversation.getProjectId().equals(projectId) && conversation.getShopId().equals(shopId))
				.findFirst();
	}

	@Override
	public Conversation create(Conversation conversation) {
		conversations.put(conversation.getId(), conversation);
		return conversation;
	}

	@Override
	public boolean startRunning(UUID conversationId) {
		var conversation = conversations.get(conversationId);
		if (conversation.isRunning()) {
			return false;
		}
		conversations.put(conversationId, conversation.toBuilder().running(true).build());
		return true;
	}

	@Override
	public void stopRunning(UUID conversationId) {
		conversations.computeIfPresent(conversationId, (id, conversation) -> conversation.toBuilder().running(false).build());
	}

	@Override
	public List<Conversation> findRunning() {
		return conversations.values().stream().filter(Conversation::isRunning).toList();
	}

	@Override
	public List<ConversationMessage> findMessages(UUID conversationId) {
		return messages.stream().filter(message -> message.getConversationId().equals(conversationId)).toList();
	}

	@Override
	public ConversationMessage addMessage(ConversationMessage message) {
		messages.add(message);
		return message;
	}

	@Override
	public Optional<DesignProposal> findOpenProposal(UUID projectId, UUID shopId) {
		return proposals.values().stream()
				.filter(proposal -> proposal.getProjectId().equals(projectId) && proposal.getStatus() == ProposalStatus.OPEN)
				.findFirst();
	}

	@Override
	public Optional<DesignProposal> findCurrentProposal(UUID projectId, UUID shopId) {
		return findOpenProposal(projectId, shopId).or(() -> proposals.values().stream()
				.filter(proposal -> proposal.getProjectId().equals(projectId) && proposal.getStatus() == ProposalStatus.ACCEPTED)
				.findFirst());
	}

	@Override
	public DesignProposal findProposal(UUID proposalId, UUID shopId) {
		return Optional.ofNullable(proposals.get(proposalId)).orElseThrow(NoSuchElementException::new);
	}

	@Override
	public DesignProposal saveProposal(DesignProposal proposal) {
		proposals.put(proposal.getId(), proposal);
		return proposal;
	}
}
