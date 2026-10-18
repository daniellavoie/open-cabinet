package dev.daniellavoie.opencabinet.assistant.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Member's message; the assistant answers in {@code language} (fr or en).
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class SendMessageRequest {
	String text;
	String language;
}
