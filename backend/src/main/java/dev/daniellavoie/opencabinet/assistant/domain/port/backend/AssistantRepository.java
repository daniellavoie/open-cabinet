package dev.daniellavoie.opencabinet.assistant.domain.port.backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantUsage;

public interface AssistantRepository {
	Optional<AssistantSettings> findSettings(UUID shopId);

	AssistantSettings saveSettings(AssistantSettings settings);

	void recordUsage(AssistantUsage usage);

	BigDecimal spentSince(UUID shopId, LocalDateTime since);
}
