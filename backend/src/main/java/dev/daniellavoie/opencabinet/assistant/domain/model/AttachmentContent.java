package dev.daniellavoie.opencabinet.assistant.domain.model;

/**
 * An attachment's bytes, with the type detected from them.
 */
public record AttachmentContent(String fileName, String mediaType, byte[] bytes) {
}
