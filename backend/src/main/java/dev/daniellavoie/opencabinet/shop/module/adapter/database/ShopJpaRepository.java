package dev.daniellavoie.opencabinet.shop.module.adapter.database;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.shop.module.adapter.database.entity.ShopEntity;

public interface ShopJpaRepository extends JpaRepository<ShopEntity, UUID> {
	@Modifying
	@Query("UPDATE Shop s SET s.quoteSequence = s.quoteSequence + 1 WHERE s.id = :shopId")
	int incrementQuoteSequence(@Param("shopId") UUID shopId);
}
