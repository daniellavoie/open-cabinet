package dev.daniellavoie.opencabinet.project.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CreateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.ProjectLock;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;

public interface ProjectDomain {
	Page<Project> findByPrincipal(Principal principal, Pageable pageable);

	Project findById(UUID projectId, Principal principal);

	Project findById(UUID projectId, UUID shopId);

	Project create(CreateProjectRequest request, Principal principal);

	/**
	 * Saves a Project. Requires the editing lock; rejected with the broken Construction Rules when
	 * the design cannot be built.
	 */
	Project update(UUID projectId, UpdateProjectRequest request, Principal principal);

	/**
	 * Every published Template Version of the starter library.
	 */
	List<CabinetTemplate> templates();

	/**
	 * Every Template Version a Project's Cabinets can use: the starter library's and those of the
	 * templates designed for it.
	 */
	List<CabinetTemplate> templates(Project project);

	List<CabinetTemplate> templates(UUID projectId, Principal principal);

	/**
	 * Takes or renews the editing lock when it is free, lapsed or already ours; otherwise reports
	 * who holds it.
	 */
	ProjectLock lock(UUID projectId, Principal principal);

	void unlock(UUID projectId, Principal principal);
}
