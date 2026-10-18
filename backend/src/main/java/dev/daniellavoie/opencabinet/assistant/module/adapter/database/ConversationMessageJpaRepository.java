package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.ConversationMessageEntity;

public interface ConversationMessageJpaRepository extends JpaRepository<ConversationMessageEntity, UUID> {
	List<ConversationMessageEntity> findByConversationIdOrderByCreationTimestamp(UUID conversationId);
}
