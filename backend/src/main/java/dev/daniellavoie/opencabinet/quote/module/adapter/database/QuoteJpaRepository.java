package dev.daniellavoie.opencabinet.quote.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.quote.module.adapter.database.entity.QuoteEntity;

public interface QuoteJpaRepository extends JpaRepository<QuoteEntity, UUID> {
	List<QuoteEntity> findByProjectIdAndShopIdOrderByNumberDesc(UUID projectId, UUID shopId);

	Optional<QuoteEntity> findByIdAndShopId(UUID id, UUID shopId);
}
