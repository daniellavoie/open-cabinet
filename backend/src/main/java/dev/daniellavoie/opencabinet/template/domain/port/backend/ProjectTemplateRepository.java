package dev.daniellavoie.opencabinet.template.domain.port.backend;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;

public interface ProjectTemplateRepository {
	List<ProjectTemplate> findByProject(UUID projectId, UUID shopId);

	/**
	 * The Shop library: templates shared with the whole Shop.
	 */
	List<ProjectTemplate> findShared(UUID shopId);

	ProjectTemplate findById(UUID templateId, UUID shopId);

	ProjectTemplate save(ProjectTemplate template, UUID shopId);

	/**
	 * Adds a Template Version; published versions are never changed or removed.
	 */
	void addVersion(UUID templateId, CabinetTemplate version, UUID shopId);

	void share(UUID templateId, UUID shopId);

	void setArchived(UUID templateId, boolean archived, UUID shopId);

	/**
	 * Removes a template that was never published.
	 */
	void delete(UUID templateId, UUID shopId);
}
