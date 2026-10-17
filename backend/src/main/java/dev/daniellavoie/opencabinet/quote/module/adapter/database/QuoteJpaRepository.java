package dev.daniellavoie.opencabinet.quote.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;

import dev.daniellavoie.opencabinet.quote.module.adapter.database.entity.QuoteEntity;

public interface QuoteJpaRepository extends JpaRepository<QuoteEntity, UUID> {
	List<QuoteEntity> findByProjectIdAndShopIdOrderByNumberDescRevisionDesc(UUID projectId, UUID shopId);

	Optional<QuoteEntity> findFirstByShopIdAndNumberOrderByRevisionDesc(UUID shopId, int number);

	@Modifying
	@Query("""
			UPDATE Quote q SET q.status = :superseded
			WHERE q.shopId = :shopId AND q.number = :number AND q.revision < :revision AND q.status = :sent""")
	int supersede(@Param("shopId") UUID shopId, @Param("number") int number, @Param("revision") int revision,
			@Param("sent") QuoteStatus sent, @Param("superseded") QuoteStatus superseded);

	Optional<QuoteEntity> findByIdAndShopId(UUID id, UUID shopId);

	List<QuoteEntity> findByShopIdAndStatusOrderByNumberAsc(UUID shopId, QuoteStatus status);
}
