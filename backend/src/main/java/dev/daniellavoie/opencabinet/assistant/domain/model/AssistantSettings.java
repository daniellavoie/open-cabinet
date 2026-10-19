package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Shop's choice to use the Design Assistant (ADR 0004): off until an Owner opts in, within a
 * monthly budget in US dollars.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class AssistantSettings {
	UUID shopId;
	boolean enabled;
	BigDecimal monthlyBudgetUsd;
	/**
	 * The Shop Style: how the Shop likes its kitchens, followed before the Design Guidelines.
	 */
	String styleText;
	/**
	 * The Owner who last turned the assistant on, and when.
	 */
	UUID enabledBy;
	LocalDateTime enabledTimestamp;
}
