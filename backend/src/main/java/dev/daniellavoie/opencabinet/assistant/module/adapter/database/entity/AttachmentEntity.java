package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Entity(name = "AssistantAttachment")
@Table(name = "assistant_attachment")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentEntity {
	@Id
	UUID id;
	UUID shopId;
	UUID conversationId;
	UUID messageId;
	String fileName;
	String mediaType;
	long sizeBytes;
	@Basic(fetch = FetchType.LAZY)
	byte[] content;
	LocalDateTime creationTimestamp;
}
