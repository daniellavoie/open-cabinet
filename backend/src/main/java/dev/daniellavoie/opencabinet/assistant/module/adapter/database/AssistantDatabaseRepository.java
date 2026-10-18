package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantUsage;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantRepository;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AssistantSettingsEntity;
import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AssistantUsageEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssistantDatabaseRepository implements AssistantRepository {
	AssistantSettingsJpaRepository settingsRepository;
	AssistantUsageJpaRepository usageRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<AssistantSettings> findSettings(UUID shopId) {
		return settingsRepository.findById(shopId).map(AssistantSettingsEntity::to);
	}

	@Override
	@Transactional
	public AssistantSettings saveSettings(AssistantSettings settings) {
		log.info("Saving design assistant settings for shop {} with: { enabled = {}, monthlyBudgetUsd = {} }",
				settings.getShopId(), settings.isEnabled(), settings.getMonthlyBudgetUsd());

		return settingsRepository.save(AssistantSettingsEntity.builder()
				.shopId(settings.getShopId())
				.enabled(settings.isEnabled())
				.monthlyBudgetUsd(settings.getMonthlyBudgetUsd())
				.enabledBy(settings.getEnabledBy())
				.enabledTimestamp(settings.getEnabledTimestamp())
				.updateTimestamp(LocalDateTime.now())
				.build())
				.to();
	}

	@Override
	@Transactional
	public void recordUsage(AssistantUsage usage) {
		log.info("Recording design assistant usage for shop {} with: { purpose = {}, model = {}, inputTokens = {}, outputTokens = {}, costUsd = {} }",
				usage.getShopId(), usage.getPurpose(), usage.getModel(), usage.getInputTokens(), usage.getOutputTokens(),
				usage.getCostUsd());

		usageRepository.save(AssistantUsageEntity.builder()
				.id(usage.getId())
				.shopId(usage.getShopId())
				.memberId(usage.getMemberId())
				.purpose(usage.getPurpose())
				.model(usage.getModel())
				.inputTokens(usage.getInputTokens())
				.outputTokens(usage.getOutputTokens())
				.costUsd(usage.getCostUsd())
				.creationTimestamp(usage.getCreationTimestamp())
				.build());
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal spentSince(UUID shopId, LocalDateTime since) {
		return usageRepository.sumCost(shopId, since);
	}
}
