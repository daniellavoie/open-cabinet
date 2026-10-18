package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * What a Member sees of the Design Assistant: whether the server can reach a model at all, whether
 * the Shop opted in, and the month's spending against its budget.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class AssistantStatus {
	/**
	 * The server has a model provider key; without one the assistant cannot be turned on.
	 */
	boolean configured;
	boolean enabled;
	BigDecimal monthlyBudgetUsd;
	BigDecimal spentThisMonthUsd;
	String enabledByName;
	LocalDateTime enabledTimestamp;
	/**
	 * The model that builds Design Proposals, and the one that sorts the Inbox.
	 */
	String builderModel;
	String sorterModel;
}
