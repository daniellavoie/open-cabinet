package dev.daniellavoie.opencabinet.assistant.module.adapter.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.daniellavoie.opencabinet.assistant.module.adapter.database.entity.AssistantUsageEntity;

public interface AssistantUsageJpaRepository extends JpaRepository<AssistantUsageEntity, UUID> {
	@Query("SELECT COALESCE(SUM(usage.costUsd), 0) FROM AssistantUsage usage WHERE usage.shopId = :shopId AND usage.creationTimestamp >= :since")
	BigDecimal sumCost(UUID shopId, LocalDateTime since);
}
