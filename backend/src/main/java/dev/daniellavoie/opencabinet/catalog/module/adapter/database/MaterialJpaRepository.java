package dev.daniellavoie.opencabinet.catalog.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.catalog.module.adapter.database.entity.MaterialEntity;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, UUID> {
	List<MaterialEntity> findByShopIdOrderByKindAscNameAsc(UUID shopId);

	Optional<MaterialEntity> findByIdAndShopId(UUID id, UUID shopId);
}
