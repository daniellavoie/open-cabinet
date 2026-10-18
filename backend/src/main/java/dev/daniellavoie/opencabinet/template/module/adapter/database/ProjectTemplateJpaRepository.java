package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ProjectTemplateEntity;

public interface ProjectTemplateJpaRepository extends JpaRepository<ProjectTemplateEntity, UUID> {
	List<ProjectTemplateEntity> findByProjectIdAndShopIdOrderByName(UUID projectId, UUID shopId);

	List<ProjectTemplateEntity> findByProjectId(UUID projectId);

	Optional<ProjectTemplateEntity> findByIdAndShopId(UUID id, UUID shopId);
}
