package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * What the Design Assistant is told before a Member's request: its role, the rules it works under,
 * and the Project's context. It reads the design, the templates it may add, the Construction
 * Standard and the names of materials and Front styles, and the Client's name; never prices, Quotes
 * or other Projects (ADR 0004).
 */
final class AssistantInstructions {
	private static final String ROLE = """
			You are the Design Assistant of Open Cabinet, a cabinet shop's design tool. A member of the shop asks you to \
			change a project's design: its room (walls, windows, doorways, appliances) and the cabinets placed in it.

			How you work:
			- You never change the project. You build a Design Proposal with the tools; a member reviews it in 3D and \
			accepts or discards it.
			- Each tool call is one step. Every step is checked against the Construction Rules: a step that would break \
			one is refused and the design stays as it was, with the rules it would break. Fix the cause and try again, or \
			explain why it cannot be done.
			- Lengths in tools are millimetres. Talk to the member in %s.
			- Walls are laid out in order, each starting where the previous one ends, turning by angleToNextDegrees \
			(counter-clockwise, seen from above). A cabinet's offsetMm runs from its wall's start to its left side; \
			elevationMm is the height of its bottom above the floor. Cabinets stand against their wall.
			- Add cabinets only from the templates listed below, by code. Base and tall cabinets stand on the floor; wall \
			cabinets hang at their template's elevation. Keep sizes within each template's ranges, given as [minimum, \
			default, maximum]; shelves as [default, maximum].
			- Only what the member typed, or a dimensioned plan, counts as a given measurement. Whatever you choose \
			yourself (a size, a template, a position) is an assumption.
			- Before your final answer, call note_proposal once with a short summary of the proposal, the questions the \
			member should answer, and the assumptions you made. Then answer briefly; the member sees the proposal in 3D.
			- If the request is a question that needs no change, just answer it.
			- Answer in %s.

			Rule codes:
			OUTSIDE_WALL: a cabinet goes past its wall's ends. ABOVE_WALL: higher than the ceiling. OVERLAP: two cabinets \
			overlap. CORNER_OVERLAP: cabinets on two walls collide in the corner. BLOCKS_OPENING: covers a window or \
			doorway. OVERLAPS_APPLIANCE: overlaps an appliance. FRONT_BLOCKED: something stands in front of its fronts. \
			WIDTH_OUT_OF_RANGE, HEIGHT_OUT_OF_RANGE, DEPTH_OUT_OF_RANGE, SHELF_COUNT_OUT_OF_RANGE: outside the template's \
			range. BASE_NOT_ON_FLOOR: a base or tall cabinet off the floor. BELOW_FLOOR: below the floor. \
			ITEM_OUTSIDE_WALL: a window, doorway, appliance or filler past its wall. DIVIDERS_NOT_ALLOWED: the template \
			takes no dividers. SECTION_TOO_NARROW: dividers too close. INVALID_WALL, UNKNOWN_WALL, UNKNOWN_TEMPLATE: the \
			wall or template does not exist.
			""";

	private AssistantInstructions() {
	}

	record Context(String language, Units units, String projectName, String clientName, ConstructionStandard standard,
			Map<String, String> standardMaterials, List<String> frontStyles, List<CabinetTemplate> addableTemplates,
			Design design, boolean continuing) {
	}

	static String build(Context context, ObjectMapper mapper) {
		var language = "en".equals(context.language()) ? "English" : "French";
		var units = context.units() == Units.METRIC ? "millimetres and centimetres" : "inches and feet (25.4 mm to the inch)";

		var facts = new LinkedHashMap<String, Object>();
		facts.put("project", context.projectName());
		facts.put("client", context.clientName());
		facts.put("constructionStandard", Map.of(
				"doorGapMm", context.standard().getDoorGapMm(),
				"toeKickHeightMm", context.standard().getToeKickHeightMm(),
				"materials", context.standardMaterials()));
		facts.put("orderedFrontStyles", context.frontStyles());
		facts.put("templates", context.addableTemplates().stream().map(AssistantInstructions::template).toList());

		return ROLE.formatted(units, language)
				+ "\nShop and project:\n" + mapper.writeValueAsString(facts)
				+ (context.continuing()
						? "\n\nThe open Design Proposal's current design (continue from it):\n"
						: "\n\nThe project's current design:\n")
				+ mapper.writeValueAsString(context.design());
	}

	private static Map<String, Object> template(CabinetTemplate template) {
		var summary = new LinkedHashMap<String, Object>();
		summary.put("code", template.getCode());
		if (template.getName() != null) {
			summary.put("name", template.getName());
		}
		summary.put("type", template.getType());
		summary.put("widthMm", List.of(template.getMinWidthMm(), template.getDefaultWidthMm(), template.getMaxWidthMm()));
		summary.put("heightMm", List.of(template.getMinHeightMm(), template.getDefaultHeightMm(), template.getMaxHeightMm()));
		summary.put("depthMm", List.of(template.getMinDepthMm(), template.getDefaultDepthMm(), template.getMaxDepthMm()));
		summary.put("elevationMm", template.getDefaultElevationMm());
		summary.put("shelves", List.of(template.getDefaultShelfCount(), template.getMaxShelfCount()));
		if (template.getBlindWidthMm() > 0) {
			summary.put("blindWidthMm", template.getBlindWidthMm());
		}
		return summary;
	}
}
