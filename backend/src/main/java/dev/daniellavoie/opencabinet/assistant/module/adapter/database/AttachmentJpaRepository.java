package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AttachmentEntity;

public interface AttachmentJpaRepository extends JpaRepository<AttachmentEntity, UUID> {
	/**
	 * An attachment without its content.
	 */
	interface Summary {
		UUID getId();

		UUID getShopId();

		UUID getConversationId();

		UUID getMessageId();

		String getFileName();

		String getMediaType();

		long getSizeBytes();

		LocalDateTime getCreationTimestamp();
	}

	List<Summary> findByMessageIdInOrderByCreationTimestamp(Collection<UUID> messageIds);

	List<AttachmentEntity> findAllByMessageIdInOrderByCreationTimestamp(Collection<UUID> messageIds);

	Optional<AttachmentEntity> findByIdAndShopId(UUID id, UUID shopId);
}
