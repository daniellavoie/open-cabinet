package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.assistant.domain.model.Attachment;
import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AttachmentStore;

/**
 * Attachments kept in memory.
 */
public final class InMemoryAttachments implements AttachmentStore {
	private final Map<UUID, Attachment> attachments = new LinkedHashMap<>();
	private final Map<UUID, byte[]> contents = new HashMap<>();

	@Override
	public Attachment save(Attachment attachment, byte[] content) {
		var saved = attachment.toBuilder().sizeBytes(content.length).build();
		attachments.put(saved.getId(), saved);
		contents.put(saved.getId(), content);
		return saved;
	}

	@Override
	public Map<UUID, List<Attachment>> findByMessages(Collection<UUID> messageIds) {
		return attachments.values().stream().filter(attachment -> messageIds.contains(attachment.getMessageId()))
				.collect(Collectors.groupingBy(Attachment::getMessageId));
	}

	@Override
	public Optional<AttachmentContent> content(UUID attachmentId, UUID shopId) {
		return Optional.ofNullable(attachments.get(attachmentId))
				.map(attachment -> new AttachmentContent(attachment.getFileName(), attachment.getMediaType(),
						contents.get(attachmentId)));
	}

	@Override
	public Map<UUID, List<AttachmentContent>> contents(Collection<UUID> messageIds) {
		return attachments.values().stream().filter(attachment -> messageIds.contains(attachment.getMessageId()))
				.collect(Collectors.groupingBy(Attachment::getMessageId,
						Collectors.mapping(attachment -> content(attachment.getId(), null).orElseThrow(),
								Collectors.toList())));
	}
}
