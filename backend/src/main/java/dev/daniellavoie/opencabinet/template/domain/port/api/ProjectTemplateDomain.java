package dev.daniellavoie.opencabinet.template.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.template.domain.model.CreateProjectTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.PublishTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.UpdateProjectTemplateRequest;

/**
 * Cabinet Templates Members design for a Project. Owners and Designers alike.
 */
public interface ProjectTemplateDomain {
	List<ProjectTemplate> findByProject(UUID projectId, Principal principal);

	ProjectTemplate findById(UUID templateId, Principal principal);

	ProjectTemplate create(UUID projectId, CreateProjectTemplateRequest request, Principal principal);

	/**
	 * Saves the draft as it is; nothing is checked until it is published.
	 */
	ProjectTemplate update(UUID templateId, UpdateProjectTemplateRequest request, Principal principal);

	/**
	 * Publishes the draft as the next Template Version, if it passes the template rules with the
	 * Shop's materials.
	 */
	ProjectTemplate publish(UUID templateId, PublishTemplateRequest request, Principal principal);
}
