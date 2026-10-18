package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "AssistantConversation")
@Table(name = "assistant_conversation")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID projectId;
	boolean running;
	LocalDateTime creationTimestamp;

	public Conversation to() {
		return Conversation.builder()
				.id(id)
				.shopId(shopId)
				.projectId(projectId)
				.running(running)
				.creationTimestamp(creationTimestamp)
				.build();
	}
}
