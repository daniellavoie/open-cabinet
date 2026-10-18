package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A photo, sketch or PDF a Member added to a Conversation message. Its content is read separately.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Attachment {
	UUID id;
	UUID shopId;
	UUID conversationId;
	UUID messageId;
	String fileName;
	String mediaType;
	long sizeBytes;
	LocalDateTime creationTimestamp;
}
