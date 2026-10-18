package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Member's message, or the assistant's answer: its text, or why it could not answer, and the
 * Design Proposal it built or changed.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ConversationMessage {
	UUID id;
	UUID conversationId;
	Author author;
	UUID memberId;
	/**
	 * Set when read.
	 */
	String memberName;
	String text;
	AnswerError error;
	UUID proposalId;
	/**
	 * Set when read.
	 */
	List<Attachment> attachments;
	LocalDateTime creationTimestamp;
}
