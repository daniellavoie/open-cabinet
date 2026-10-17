package dev.daniellavoie.opencabinet.project.module.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CreateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.ProjectLock;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {
	ProjectDomain domain;

	@GetMapping("/project")
	public Page<Project> findAll(Principal principal,
			@PageableDefault(size = 50, sort = "updateTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
		return domain.findByPrincipal(principal, pageable);
	}

	@GetMapping("/project/{projectId}")
	public Project findById(@PathVariable UUID projectId, Principal principal) {
		return domain.findById(projectId, principal);
	}

	@PostMapping("/project")
	public Project create(@RequestBody CreateProjectRequest request, Principal principal) {
		return domain.create(request, principal);
	}

	@PutMapping("/project/{projectId}")
	public Project update(@PathVariable UUID projectId, @RequestBody UpdateProjectRequest request,
			Principal principal) {
		return domain.update(projectId, request, principal);
	}

	@PostMapping("/project/{projectId}/lock")
	public ProjectLock lock(@PathVariable UUID projectId, Principal principal) {
		return domain.lock(projectId, principal);
	}

	@DeleteMapping("/project/{projectId}/lock")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlock(@PathVariable UUID projectId, Principal principal) {
		domain.unlock(projectId, principal);
	}

	@GetMapping("/cabinet-template")
	public List<CabinetTemplate> templates() {
		return domain.templates();
	}
}
