package dev.daniellavoie.opencabinet.client.module.adapter.database;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.client.module.adapter.database.entity.ClientEntity;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {
	Page<ClientEntity> findByShopId(UUID shopId, Pageable pageable);

	Optional<ClientEntity> findByIdAndShopId(UUID id, UUID shopId);
}
