package dev.daniellavoie.opencabinet.template.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.template.domain.model.CreateProjectTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.ProjectTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.PublishTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.model.UpdateProjectTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.port.api.ProjectTemplateDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectTemplateController {
	ProjectTemplateDomain domain;

	@GetMapping("/project/{projectId}/project-template")
	public List<ProjectTemplate> findByProject(@PathVariable UUID projectId, Principal principal) {
		return domain.findByProject(projectId, principal);
	}

	@PostMapping("/project/{projectId}/project-template")
	public ProjectTemplate create(@PathVariable UUID projectId, @RequestBody CreateProjectTemplateRequest request,
			Principal principal) {
		return domain.create(projectId, request, principal);
	}

	/**
	 * The Shop library.
	 */
	@GetMapping("/template")
	public List<ProjectTemplate> findShared(Principal principal) {
		return domain.findShared(principal);
	}

	@PostMapping("/template")
	public ProjectTemplate createInLibrary(@RequestBody CreateProjectTemplateRequest request, Principal principal) {
		return domain.createInLibrary(request, principal);
	}

	@PostMapping("/project-template/{templateId}/share")
	public ProjectTemplate share(@PathVariable UUID templateId, Principal principal) {
		return domain.share(templateId, principal);
	}

	@PutMapping("/project-template/{templateId}/archived")
	public ProjectTemplate setArchived(@PathVariable UUID templateId, @RequestBody boolean archived,
			Principal principal) {
		return domain.setArchived(templateId, archived, principal);
	}

	@DeleteMapping("/project-template/{templateId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID templateId, Principal principal) {
		domain.delete(templateId, principal);
	}

	@GetMapping("/project-template/{templateId}")
	public ProjectTemplate findById(@PathVariable UUID templateId, Principal principal) {
		return domain.findById(templateId, principal);
	}

	@PutMapping("/project-template/{templateId}")
	public ProjectTemplate update(@PathVariable UUID templateId, @RequestBody UpdateProjectTemplateRequest request,
			Principal principal) {
		return domain.update(templateId, request, principal);
	}

	@PostMapping("/project-template/{templateId}/publish")
	public ProjectTemplate publish(@PathVariable UUID templateId, @RequestBody PublishTemplateRequest request,
			Principal principal) {
		return domain.publish(templateId, request, principal);
	}
}
