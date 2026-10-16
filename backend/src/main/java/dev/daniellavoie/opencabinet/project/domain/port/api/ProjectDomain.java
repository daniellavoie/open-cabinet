package dev.daniellavoie.opencabinet.project.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CreateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;

public interface ProjectDomain {
	Page<Project> findByPrincipal(Principal principal, Pageable pageable);

	Project findById(UUID projectId, Principal principal);

	Project findById(UUID projectId, UUID shopId);

	Project create(CreateProjectRequest request, Principal principal);

	/**
	 * Saves a Project. Rejected with the broken Construction Rules when the design cannot be built.
	 */
	Project update(UUID projectId, UpdateProjectRequest request, Principal principal);

	List<CabinetTemplate> templates();
}
