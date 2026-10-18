package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.port.backend.PublishedTemplateRepository;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ProjectTemplateRepository;
import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ProjectTemplateEntity;
import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ProjectTemplateVersionEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectTemplateDatabaseRepository implements ProjectTemplateRepository, PublishedTemplateRepository {
	ProjectTemplateJpaRepository jpaRepository;
	ProjectTemplateVersionJpaRepository versionRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ProjectTemplate> findByProject(UUID projectId, UUID shopId) {
		return withVersions(jpaRepository.findByProjectIdAndShopIdOrderByName(projectId, shopId));
	}

	@Override
	@Transactional(readOnly = true)
	public ProjectTemplate findById(UUID templateId, UUID shopId) {
		return withVersions(List.of(find(templateId, shopId))).get(0);
	}

	@Override
	@Transactional
	public ProjectTemplate save(ProjectTemplate template, UUID shopId) {
		var existing = jpaRepository.findByIdAndShopId(template.getId(), shopId);
		var now = LocalDateTime.now();
		jpaRepository.save(ProjectTemplateEntity.builder()
				.id(template.getId())
				.shopId(shopId)
				.projectId(existing.map(ProjectTemplateEntity::getProjectId).orElse(template.getProjectId()))
				.name(template.getName())
				.draft(template.getDraft())
				.sourceCabinetId(existing.map(ProjectTemplateEntity::getSourceCabinetId)
						.orElse(template.getSourceCabinetId()))
				.creationTimestamp(existing.map(ProjectTemplateEntity::getCreationTimestamp).orElse(now))
				.updateTimestamp(now)
				.build());
		return findById(template.getId(), shopId);
	}

	@Override
	@Transactional
	public void addVersion(UUID templateId, CabinetTemplate version, UUID shopId) {
		find(templateId, shopId);
		var key = new ProjectTemplateVersionEntity.Key(templateId, version.getVersion());
		if (versionRepository.existsById(key)) {
			throw new IllegalStateException("version " + version.getVersion() + " is already published");
		}
		versionRepository.save(ProjectTemplateVersionEntity.builder()
				.key(key)
				.content(version)
				.publishTimestamp(LocalDateTime.now())
				.build());
		log.info("Published template {} version {} for shop {}", templateId, version.getVersion(), shopId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CabinetTemplate> findPublished(UUID projectId) {
		var ids = jpaRepository.findByProjectId(projectId).stream().map(ProjectTemplateEntity::getId).toList();
		return ids.isEmpty() ? List.of()
				: versionRepository.findByKeyTemplateIdInOrderByKeyVersion(ids).stream()
						.map(ProjectTemplateVersionEntity::getContent)
						.toList();
	}

	private ProjectTemplateEntity find(UUID templateId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(templateId, shopId)
				.orElseThrow(() -> new NoSuchElementException("template " + templateId + " not found"));
	}

	private List<ProjectTemplate> withVersions(List<ProjectTemplateEntity> entities) {
		Map<UUID, List<CabinetTemplate>> versions = entities.isEmpty() ? Map.of()
				: versionRepository.findByKeyTemplateIdInOrderByKeyVersion(
						entities.stream().map(ProjectTemplateEntity::getId).toList()).stream()
						.collect(Collectors.groupingBy(version -> version.getKey().getTemplateId(),
								Collectors.mapping(ProjectTemplateVersionEntity::getContent, Collectors.toList())));
		return entities.stream()
				.map(entity -> ProjectTemplate.builder()
						.id(entity.getId())
						.projectId(entity.getProjectId())
						.name(entity.getName())
						.draft(entity.getDraft())
						.versions(versions.getOrDefault(entity.getId(), List.of()))
						.sourceCabinetId(entity.getSourceCabinetId())
						.creationTimestamp(entity.getCreationTimestamp())
						.updateTimestamp(entity.getUpdateTimestamp())
						.build())
				.toList();
	}
}
