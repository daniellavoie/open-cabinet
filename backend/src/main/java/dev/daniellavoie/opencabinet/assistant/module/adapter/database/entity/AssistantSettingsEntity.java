package dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "AssistantSettings")
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssistantSettingsEntity {
	@Id
	UUID shopId;
	boolean enabled;
	BigDecimal monthlyBudgetUsd;
	UUID enabledBy;
	LocalDateTime enabledTimestamp;
	LocalDateTime updateTimestamp;

	public AssistantSettings to() {
		return AssistantSettings.builder()
				.shopId(shopId)
				.enabled(enabled)
				.monthlyBudgetUsd(monthlyBudgetUsd)
				.enabledBy(enabledBy)
				.enabledTimestamp(enabledTimestamp)
				.build();
	}
}
