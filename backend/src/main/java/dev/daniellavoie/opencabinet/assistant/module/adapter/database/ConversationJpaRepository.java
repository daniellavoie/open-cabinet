package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.ConversationEntity;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {
	Optional<ConversationEntity> findFirstByProjectIdAndShopIdOrderByCreationTimestamp(UUID projectId, UUID shopId);

	List<ConversationEntity> findByRunningTrue();

	@Modifying
	@Query("UPDATE AssistantConversation conversation SET conversation.running = true WHERE conversation.id = :id AND conversation.running = false")
	int startRunning(@Param("id") UUID id);

	@Modifying
	@Query("UPDATE AssistantConversation conversation SET conversation.running = false WHERE conversation.id = :id")
	int stopRunning(@Param("id") UUID id);
}
