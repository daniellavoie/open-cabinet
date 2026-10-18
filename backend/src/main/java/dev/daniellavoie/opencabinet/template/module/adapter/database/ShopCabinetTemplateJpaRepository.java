package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ShopCabinetTemplateEntity;

public interface ShopCabinetTemplateJpaRepository extends JpaRepository<ShopCabinetTemplateEntity, UUID> {
	List<ShopCabinetTemplateEntity> findByShopIdOrderByName(UUID shopId);

	@Query("SELECT DISTINCT template.shopId FROM ShopCabinetTemplate template")
	List<UUID> findShopIds();

	Optional<ShopCabinetTemplateEntity> findByIdAndShopId(UUID id, UUID shopId);
}
