package dev.daniellavoie.opencabinet.project.module.adapter.database;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.project.module.adapter.database.entity.ProjectEntity;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {
	Page<ProjectEntity> findByShopId(UUID shopId, Pageable pageable);

	Optional<ProjectEntity> findByIdAndShopId(UUID id, UUID shopId);
}
