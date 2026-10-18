package dev.daniellavoie.opencabinet.assistant.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class UpdateAssistantSettingsRequest {
	boolean enabled;
	BigDecimal monthlyBudgetUsd;
}
