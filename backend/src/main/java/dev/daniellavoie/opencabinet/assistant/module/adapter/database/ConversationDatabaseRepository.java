package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.ConversationRepository;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.ConversationEntity;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.ConversationMessageEntity;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.DesignProposalEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationDatabaseRepository implements ConversationRepository {
	ConversationJpaRepository conversations;
	ConversationMessageJpaRepository messages;
	DesignProposalJpaRepository proposals;

	@Override
	@Transactional(readOnly = true)
	public Optional<Conversation> findByProject(UUID projectId, UUID shopId) {
		return conversations.findFirstByProjectIdAndShopIdOrderByCreationTimestamp(projectId, shopId).map(ConversationEntity::to);
	}

	@Override
	@Transactional
	public Conversation create(Conversation conversation) {
		log.info("Creating conversation {} with: { shopId = {}, projectId = {} }", conversation.getId(), conversation.getShopId(),
				conversation.getProjectId());

		return conversations.save(ConversationEntity.builder()
				.id(conversation.getId())
				.shopId(conversation.getShopId())
				.projectId(conversation.getProjectId())
				.running(false)
				.creationTimestamp(conversation.getCreationTimestamp())
				.build())
				.to();
	}

	@Override
	@Transactional
	public boolean startRunning(UUID conversationId) {
		return conversations.startRunning(conversationId) == 1;
	}

	@Override
	@Transactional
	public void stopRunning(UUID conversationId) {
		conversations.stopRunning(conversationId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Conversation> findRunning() {
		return conversations.findByRunningTrue().stream().map(ConversationEntity::to).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ConversationMessage> findMessages(UUID conversationId) {
		return messages.findByConversationIdOrderByCreationTimestamp(conversationId).stream()
				.map(ConversationMessageEntity::to)
				.toList();
	}

	@Override
	@Transactional
	public ConversationMessage addMessage(ConversationMessage message) {
		return messages.save(ConversationMessageEntity.builder()
				.id(message.getId())
				.conversationId(message.getConversationId())
				.author(message.getAuthor())
				.memberId(message.getMemberId())
				.text(message.getText())
				.error(message.getError())
				.proposalId(message.getProposalId())
				.creationTimestamp(message.getCreationTimestamp())
				.build())
				.to();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DesignProposal> findOpenProposal(UUID projectId, UUID shopId) {
		return proposals.findFirstByProjectIdAndShopIdAndStatusOrderByUpdateTimestampDesc(projectId, shopId, ProposalStatus.OPEN)
				.map(DesignProposalEntity::to);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DesignProposal> findCurrentProposal(UUID projectId, UUID shopId) {
		return findOpenProposal(projectId, shopId)
				.or(() -> proposals.findFirstByProjectIdAndShopIdAndStatusOrderByUpdateTimestampDesc(projectId, shopId,
						ProposalStatus.ACCEPTED).map(DesignProposalEntity::to));
	}

	@Override
	@Transactional(readOnly = true)
	public DesignProposal findProposal(UUID proposalId, UUID shopId) {
		return proposals.findByIdAndShopId(proposalId, shopId)
				.map(DesignProposalEntity::to)
				.orElseThrow(() -> new NoSuchElementException("design proposal " + proposalId + " not found"));
	}

	@Override
	@Transactional
	public DesignProposal saveProposal(DesignProposal proposal) {
		log.info("Saving design proposal {} with: { projectId = {}, status = {}, steps = {} }", proposal.getId(),
				proposal.getProjectId(), proposal.getStatus(), proposal.getSteps().size());

		return proposals.save(DesignProposalEntity.from(proposal)).to();
	}
}
