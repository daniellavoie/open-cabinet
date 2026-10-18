package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

/**
 * A Project's Conversation with the Design Assistant, kept with the Project for every Member.
 */
@Value
@Builder(toBuilder = true)
public class Conversation {
	UUID id;
	UUID shopId;
	UUID projectId;
	/**
	 * A request is being answered; the next waits for it.
	 */
	boolean running;
	LocalDateTime creationTimestamp;
}
