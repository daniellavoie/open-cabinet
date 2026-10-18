package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ProjectTemplateEntity;

public interface ProjectTemplateJpaRepository extends JpaRepository<ProjectTemplateEntity, UUID> {
	List<ProjectTemplateEntity> findByProjectIdAndShopIdOrderByName(UUID projectId, UUID shopId);

	/**
	 * A Project's own templates and its Shop's shared ones.
	 */
	@Query(value = "SELECT * FROM project_template WHERE project_id = :projectId OR (shared_timestamp IS NOT NULL"
			+ " AND shop_id = (SELECT shop_id FROM project WHERE id = :projectId))", nativeQuery = true)
	List<ProjectTemplateEntity> findUsableBy(@Param("projectId") UUID projectId);

	List<ProjectTemplateEntity> findByShopIdAndSharedTimestampNotNullOrderByName(UUID shopId);

	Optional<ProjectTemplateEntity> findByIdAndShopId(UUID id, UUID shopId);
}
