package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * The model's answer, the tool calls it asks for, the tokens it used and what they cost.
 */
@Value
@Builder(toBuilder = true)
public class ModelReply {
	String text;
	@Builder.Default
	List<ModelRequest.ToolCall> toolCalls = List.of();
	String model;
	/**
	 * The model stopped at the output cap: what it wrote is cut short.
	 */
	boolean truncated;
	long inputTokens;
	long outputTokens;
	BigDecimal costUsd;
}
