package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

/**
 * One call to the model, counted against the Shop's monthly budget.
 */
@Value
@Builder(toBuilder = true)
public class AssistantUsage {
	UUID id;
	UUID shopId;
	UUID memberId;
	String purpose;
	String model;
	long inputTokens;
	long outputTokens;
	BigDecimal costUsd;
	LocalDateTime creationTimestamp;
}
