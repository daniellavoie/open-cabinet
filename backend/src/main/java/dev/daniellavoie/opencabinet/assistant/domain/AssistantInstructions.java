package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
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
			- Members may attach photos, sketches and PDFs. Only numbers the member typed, or written on a dimensioned \
			plan, count as given measurements. A length you read from a photo or an undimensioned sketch is always an \
			assumption, however clear it looks; so is whatever you choose yourself (a size, a template, a position).
			- Design for a kitchen a person would want to cook in and look at, following the design guide below. After each \
			step you hear the remarks it created: fix them in your next steps.
			- Before your final answer, call note_proposal once with a short summary of the proposal, the questions the \
			member should answer, the assumptions you made, and an explanation for each remark you keep (by its key, in one \
			sentence). It returns the remarks left. Then answer briefly; the member sees the proposal in 3D.
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
			Design design, boolean continuing, String shopStyle, List<Remark> remarks) {
	}

	/**
	 * The Design Guidelines in words, with the Shop's numbers: what the remarks check, and the
	 * judgement calls they leave to the assistant.
	 */
	static String guide(Units units) {
		var metric = units == Units.METRIC;
		return """
				Design guide (each checked point gives a remark when not followed; remarks never block):
				- Runs: base cabinets form continuous runs from a corner, a wall's end or a tall unit to the next appliance, \
				opening or end. No gaps except for an appliance or a doorway: close a gap of %s or less with a filler, a wider \
				one with a cabinet. Leave no sliver under %s at a run's end.
				- Fillers only at a run's ends (against a wall, at a corner, beside a tall unit or fridge), %s wide; at least %s \
				at a corner.
				- Widths in steps of %s. Prefer few, wide cabinets, wider before narrower, and repeat widths for symmetry \
				(two equal cabinets either side of a centred sink). Doors wider than 24" (600 mm) come in pairs: that is the \
				template's choice.
				- Corners: where two base runs meet, turn the corner with a blind corner cabinet and leave %s of filler before \
				the run on the other wall. Wall cabinets stop short of the corner with a filler.
				- The sink base is centred under the window on its wall. The dishwasher stands right beside the sink base, on \
				the side with more counter, never in a corner.
				- Landing space: beside the range at least %s on one side and %s on the other; beside the sink %s and %s; beside \
				the fridge %s on at least one side, its handle side if known.
				- Work triangle between sink, range and fridge: each leg %s to %s, the three together under %s, nothing tall \
				across a leg.
				- Wall cabinets line up with the cabinets and appliances below them and share one top line per wall; never over \
				a window; over the range a hood, or a shorter wall cabinet leaving %s above the cooktop.
				- Tall units (pantry, oven tower, fridge) at a run's end, never in the middle of a counter, grouped when there \
				are several.
				- The range sits inside a run, with counter on both sides; a hood is centred over it, at least as wide.
				""".formatted(
				metric ? "75 mm" : "3\"", metric ? "300 mm" : "12\"", metric ? "25 to 75 mm" : "1\" to 3\"",
				metric ? "25 mm" : "1\"", metric ? "50 mm" : "3\"", metric ? "25 mm" : "1\"",
				metric ? "300 mm" : "12\"", metric ? "380 mm" : "15\"", metric ? "600 mm" : "24\"",
				metric ? "450 mm" : "18\"", metric ? "380 mm" : "15\"", metric ? "1.2 m" : "4'", metric ? "2.7 m" : "9'",
				metric ? "7.9 m" : "26'", metric ? "600 mm" : "24\"");
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

		var style = context.shopStyle() == null || context.shopStyle().isBlank() ? ""
				: "\nThe Shop's own style, which comes before the design guide where they differ:\n" + context.shopStyle() + "\n";
		var remarks = context.remarks().isEmpty() ? "\nThe design has no remarks.\n"
				: "\nRemarks on the design as it is now (key: what):\n" + context.remarks().stream()
						.map(remark -> "- " + remark.key() + ": " + RemarkMessages.describe(remark, context.units()))
						.collect(java.util.stream.Collectors.joining("\n")) + "\n";
		return ROLE.formatted(units, language)
				+ "\n" + guide(context.units())
				+ style
				+ remarks
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
