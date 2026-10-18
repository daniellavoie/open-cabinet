package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.AnswerError;
import dev.daniellavoie.opencabinet.assistant.domain.model.Author;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Entity(name = "AssistantMessage")
@Table(name = "assistant_message")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationMessageEntity {
	@Id
	UUID id;
	UUID conversationId;
	@Enumerated(EnumType.STRING)
	Author author;
	UUID memberId;
	String text;
	@Enumerated(EnumType.STRING)
	@Column(name = "error_code")
	AnswerError error;
	UUID proposalId;
	LocalDateTime creationTimestamp;

	public ConversationMessage to() {
		return ConversationMessage.builder()
				.id(id)
				.conversationId(conversationId)
				.author(author)
				.memberId(memberId)
				.text(text)
				.error(error)
				.proposalId(proposalId)
				.creationTimestamp(creationTimestamp)
				.build();
	}
}
