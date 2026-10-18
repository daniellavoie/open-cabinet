package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.assistant.domain.model.Attachment;
import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AttachmentStore;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AttachmentEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Attachments in PostgreSQL, their content read only when it is needed.
 */
@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttachmentDatabaseStore implements AttachmentStore {
	AttachmentJpaRepository jpaRepository;

	@Override
	@Transactional
	public Attachment save(Attachment attachment, byte[] content) {
		log.info("Saving attachment {} with: { messageId = {}, mediaType = {}, sizeBytes = {} }", attachment.getId(),
				attachment.getMessageId(), attachment.getMediaType(), content.length);

		jpaRepository.save(AttachmentEntity.builder()
				.id(attachment.getId())
				.shopId(attachment.getShopId())
				.conversationId(attachment.getConversationId())
				.messageId(attachment.getMessageId())
				.fileName(attachment.getFileName())
				.mediaType(attachment.getMediaType())
				.sizeBytes(content.length)
				.content(content)
				.creationTimestamp(attachment.getCreationTimestamp())
				.build());
		return attachment.toBuilder().sizeBytes(content.length).build();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, List<Attachment>> findByMessages(Collection<UUID> messageIds) {
		if (messageIds.isEmpty()) {
			return Map.of();
		}
		return jpaRepository.findByMessageIdInOrderByCreationTimestamp(messageIds).stream()
				.map(summary -> Attachment.builder()
						.id(summary.getId())
						.shopId(summary.getShopId())
						.conversationId(summary.getConversationId())
						.messageId(summary.getMessageId())
						.fileName(summary.getFileName())
						.mediaType(summary.getMediaType())
						.sizeBytes(summary.getSizeBytes())
						.creationTimestamp(summary.getCreationTimestamp())
						.build())
				.collect(Collectors.groupingBy(Attachment::getMessageId, LinkedHashMap::new, Collectors.toList()));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AttachmentContent> content(UUID attachmentId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(attachmentId, shopId)
				.map(entity -> new AttachmentContent(entity.getFileName(), entity.getMediaType(), entity.getContent()));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, List<AttachmentContent>> contents(Collection<UUID> messageIds) {
		if (messageIds.isEmpty()) {
			return Map.of();
		}
		return jpaRepository.findAllByMessageIdInOrderByCreationTimestamp(messageIds).stream()
				.collect(Collectors.groupingBy(AttachmentEntity::getMessageId, LinkedHashMap::new,
						Collectors.mapping(entity -> new AttachmentContent(entity.getFileName(), entity.getMediaType(),
								entity.getContent()), Collectors.toList())));
	}
}
