package dev.daniellavoie.opencabinet.project.domain.port.backend;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;

public interface PublishedTemplateRepository {
	/**
	 * Every published Template Version a Project can use besides the starter library: its own
	 * templates' and the Shop library's, archived ones marked.
	 */
	List<CabinetTemplate> findPublished(UUID projectId);
}
