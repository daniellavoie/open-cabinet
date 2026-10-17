package dev.daniellavoie.opencabinet.cutlist.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.cutlist.domain.model.CutListRow;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

public interface CutListDomain {
	ProjectOutput output(UUID projectId, Principal principal);

	/**
	 * The output of a saved Project, ready to print or export.
	 */
	OutputDocument document(UUID projectId, Language language, Principal principal);

	ProjectOutput output(Project project, ConstructionStandard standard, List<Material> catalog);

	/**
	 * Each Cabinet's own Parts to cut, by Cabinet label in reading order. Trim that belongs to the
	 * Project (Fillers, toe kicks) is left out.
	 */
	Map<String, List<CutListRow>> cabinetCutLists(Project project, ConstructionStandard standard,
			List<Material> catalog);
}
