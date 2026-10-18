package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.Attachment;
import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;

/**
 * Where Conversation attachments are kept.
 */
public interface AttachmentStore {
	Attachment save(Attachment attachment, byte[] content);

	/**
	 * The attachments of these messages, by message, in the order they were added.
	 */
	Map<UUID, List<Attachment>> findByMessages(Collection<UUID> messageIds);

	Optional<AttachmentContent> content(UUID attachmentId, UUID shopId);

	/**
	 * The contents of these messages' attachments, by message, to send them to the model.
	 */
	Map<UUID, List<AttachmentContent>> contents(Collection<UUID> messageIds);
}
