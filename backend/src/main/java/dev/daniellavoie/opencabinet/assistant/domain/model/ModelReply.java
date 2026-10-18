package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

/**
 * The model's answer, with the tokens it used and what they cost.
 */
@Value
@Builder(toBuilder = true)
public class ModelReply {
	String text;
	String model;
	long inputTokens;
	long outputTokens;
	BigDecimal costUsd;
}
