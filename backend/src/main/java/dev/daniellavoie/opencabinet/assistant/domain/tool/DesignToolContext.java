package dev.daniellavoie.opencabinet.assistant.domain.tool;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;

/**
 * What tools may use besides the design: the Template Versions the Project's Cabinets can use, the
 * newest of each template a Cabinet may be added from, the Shop's ordered Front styles by name, and
 * where new ids come from: fresh ones when building a Proposal, the recorded ones when replaying it.
 */
public record DesignToolContext(List<CabinetTemplate> templates, Map<String, CabinetTemplate> addableTemplates,
		Map<String, UUID> frontStyles, Function<String, String> newId) {
}
