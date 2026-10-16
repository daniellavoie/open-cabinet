package dev.daniellavoie.opencabinet.cutlist.module.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/output")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CutListController {
	CutListDomain domain;

	@GetMapping
	public ProjectOutput output(@PathVariable UUID projectId, Principal principal) {
		return domain.output(projectId, principal);
	}
}
