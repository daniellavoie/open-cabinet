package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ShopCabinetTemplateEntity;

public interface ShopCabinetTemplateJpaRepository extends JpaRepository<ShopCabinetTemplateEntity, UUID> {
	List<ShopCabinetTemplateEntity> findByShopIdOrderByName(UUID shopId);

	Optional<ShopCabinetTemplateEntity> findByIdAndShopId(UUID id, UUID shopId);
}
