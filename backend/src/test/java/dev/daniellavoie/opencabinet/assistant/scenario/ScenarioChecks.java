package dev.daniellavoie.opencabinet.assistant.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.project.domain.ConstructionRules;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;

/**
 * Checks a scenario's outcome against its expectations.
 */
final class ScenarioChecks {
	record Check(String name, boolean passed, String detail) {
	}

	private ScenarioChecks() {
	}

	static List<Check> check(Scenario scenario, ConversationView view, List<CabinetTemplate> templates) {
		var checks = new ArrayList<Check>();
		var expect = scenario.expect();
		var proposal = view.getProposal();
		var design = proposal == null ? scenario.design() : proposal.getDesign();
		var errors = view.getMessages().stream().map(ConversationMessage::getError).filter(error -> error != null).toList();

		checks.add(new Check("answered", errors.isEmpty(), errors.isEmpty() ? "" : errors.toString()));
		var violations = ConstructionRules.check(design, templates);
		checks.add(new Check("passes the Construction Rules", violations.isEmpty(), violations.toString()));

		range(checks, "steps", expect.steps(), proposal == null ? 0 : proposal.getSteps().size());
		range(checks, "walls", expect.walls(), list(design.getWalls()).size());
		range(checks, "cabinets", expect.cabinets(), list(design.getCabinets()).size());
		counts(checks, "template", expect.templates(), list(design.getCabinets()), Cabinet::getTemplateCode);
		counts(checks, "type", expect.types() == null ? null
				: expect.types().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)),
				list(design.getCabinets()), cabinet -> type(cabinet, templates));
		counts(checks, "opening", expect.openings(), list(design.getOpenings()), opening -> opening.getKind().name());
		counts(checks, "appliance", expect.appliances(), list(design.getAppliances()), appliance -> appliance.getKind().name());
		range(checks, "open questions", expect.openQuestions(), proposal == null ? 0 : proposal.getOpenQuestions().size());
		range(checks, "assumptions", expect.assumptions(), proposal == null ? 0 : proposal.getAssumptions().size());
		for (var under : Optional.ofNullable(expect.under()).orElse(List.of())) {
			checks.add(new Check(under.template() + " under a " + under.opening(), under(design, under), ""));
		}
		return checks;
	}

	private static void range(List<Check> checks, String name, Scenario.Range range, int count) {
		if (range != null) {
			checks.add(new Check(name + " " + range, range.contains(count), "got " + count));
		}
	}

	private static <T> void counts(List<Check> checks, String what, Map<String, Scenario.Range> expected, List<T> items,
			Function<T, String> key) {
		if (expected == null) {
			return;
		}
		var counts = items.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
		expected.forEach((name, range) -> range(checks, what + " " + name, range, counts.getOrDefault(name, 0L).intValue()));
	}

	/**
	 * At least half of the narrower of the Cabinet and the Opening overlaps the other, on one Wall.
	 */
	private static boolean under(Design design, Scenario.Under under) {
		return list(design.getCabinets()).stream()
				.filter(cabinet -> cabinet.getTemplateCode().equals(under.template()))
				.anyMatch(cabinet -> list(design.getOpenings()).stream()
						.filter(opening -> opening.getKind().name().equals(under.opening())
								&& opening.getWallId().equals(cabinet.getWallId()))
						.anyMatch(opening -> {
							var overlap = Math.min(cabinet.getOffsetMm() + cabinet.getWidthMm(), opening.getOffsetMm() + opening.getWidthMm())
									- Math.max(cabinet.getOffsetMm(), opening.getOffsetMm());
							return overlap >= Math.min(cabinet.getWidthMm(), opening.getWidthMm()) / 2;
						}));
	}

	private static String type(Cabinet cabinet, List<CabinetTemplate> templates) {
		return templates.stream()
				.filter(template -> template.getCode().equals(cabinet.getTemplateCode()) && template.getVersion() == cabinet.getTemplateVersion())
				.findFirst()
				.map(template -> template.getType().name())
				.orElse("UNKNOWN");
	}

	private static <T> List<T> list(List<T> items) {
		return items == null ? List.of() : items;
	}
}
