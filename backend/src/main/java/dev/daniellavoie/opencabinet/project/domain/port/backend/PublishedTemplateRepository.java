package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;

public interface PublishedTemplateRepository {
	/**
	 * Every published Template Version of the templates designed for a Project.
	 */
	List<CabinetTemplate> findPublished(UUID projectId);
}
