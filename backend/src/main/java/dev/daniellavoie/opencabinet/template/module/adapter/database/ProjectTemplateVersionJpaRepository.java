package dev.daniellavoie.opencabinet.template.module.adapter.database;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.daniellavoie.opencabinet.template.module.adapter.database.entity.ProjectTemplateVersionEntity;

public interface ProjectTemplateVersionJpaRepository
		extends JpaRepository<ProjectTemplateVersionEntity, ProjectTemplateVersionEntity.Key> {
	List<ProjectTemplateVersionEntity> findByKeyTemplateIdInOrderByKeyVersion(Collection<UUID> templateIds);
}
