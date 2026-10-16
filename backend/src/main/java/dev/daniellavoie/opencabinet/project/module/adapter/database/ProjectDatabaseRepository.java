package dev.daniellavoie.opencabinet.project.module.adapter.database;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.port.backend.ProjectRepository;
import dev.daniellavoie.opencabinet.project.module.adapter.database.entity.ProjectEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectDatabaseRepository implements ProjectRepository {
	ProjectJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<Project> findByShop(UUID shopId, Pageable pageable) {
		return jpaRepository.findByShopId(shopId, pageable).map(ProjectEntity::to);
	}

	@Override
	@Transactional(readOnly = true)
	public Project findById(UUID projectId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(projectId, shopId)
				.map(ProjectEntity::to)
				.orElseThrow(() -> new NoSuchElementException("project " + projectId + " not found"));
	}

	@Override
	@Transactional
	public Project save(Project project, UUID shopId) {
		var now = LocalDateTime.now();
		var entity = jpaRepository.findByIdAndShopId(project.getId(), shopId)
				.orElseGet(() -> ProjectEntity.builder()
						.id(project.getId())
						.shopId(shopId)
						.creationTimestamp(now)
						.build());

		return jpaRepository.save(entity.toBuilder()
				.clientId(project.getClientId())
				.name(project.getName())
				.design(project.getDesign())
				.labourMethod(project.getLabourMethod())
				.labourHours(project.getLabourHours())
				.manualLines(project.getManualLines())
				.updateTimestamp(now)
				.build())
				.to();
	}
}
