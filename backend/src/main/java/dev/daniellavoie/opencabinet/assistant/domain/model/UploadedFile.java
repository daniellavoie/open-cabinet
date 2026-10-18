package dev.daniellavoie.opencabinet.assistant.domain.model;

/**
 * A file as a Member sent it, before it is checked.
 */
public record UploadedFile(String fileName, byte[] bytes) {
}
