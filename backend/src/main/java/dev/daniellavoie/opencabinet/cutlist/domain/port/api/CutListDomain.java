package dev.daniellavoie.opencabinet.cutlist.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

public interface CutListDomain {
	ProjectOutput output(UUID projectId, Principal principal);

	ProjectOutput output(Project project, ConstructionStandard standard, List<Material> catalog);
}
