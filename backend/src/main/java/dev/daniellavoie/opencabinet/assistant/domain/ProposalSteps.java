package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import tools.jackson.databind.JsonNode;

import dev.daniellavoie.opencabinet.assistant.domain.model.DesignStep;
import dev.daniellavoie.opencabinet.assistant.domain.tool.DesignToolContext;
import dev.daniellavoie.opencabinet.assistant.domain.tool.DesignTools;
import dev.daniellavoie.opencabinet.project.domain.ConstructionRules;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;

/**
 * Takes a Design Proposal's steps, each checked by the Construction Rules
 * (docs/adr/0004-design-assistant-proposes-member-accepts.md): a step that breaks a rule the
 * design did not already break is refused. Building a Proposal and accepting it take steps the
 * same way.
 */
public final class ProposalSteps {
	private ProposalSteps() {
	}

	public sealed interface Result permits Taken, Refused {
	}

	public record Taken(Design design, DesignStep step) implements Result {
	}

	/**
	 * Why a step was not taken: the rules it would break, or why the tool could not use its
	 * arguments.
	 */
	public record Refused(List<Violation> violations, String problem) implements Result {
	}

	/**
	 * How far replaying went: the design after the steps taken, and the step it stopped at.
	 */
	public record Replay(Design design, int applied, Integer refusedStep, Refused refusal) {
	}

	public static Result take(Design design, String toolName, JsonNode arguments, DesignToolContext context) {
		var tool = DesignTools.BY_NAME.get(toolName);
		if (tool == null) {
			return new Refused(List.of(), "no tool named " + toolName);
		}
		try {
			var outcome = tool.apply(design, arguments, context);
			var before = new HashSet<>(ConstructionRules.check(design, context.templates()));
			var broken = ConstructionRules.check(outcome.design(), context.templates()).stream()
					.filter(violation -> !before.contains(violation))
					.toList();
			if (!broken.isEmpty()) {
				return new Refused(broken, null);
			}
			return new Taken(outcome.design(), DesignStep.builder()
					.tool(toolName)
					.arguments(arguments)
					.createdId(outcome.createdId())
					.build());
		} catch (IllegalArgumentException exception) {
			return new Refused(List.of(), exception.getMessage());
		}
	}

	/**
	 * Replays steps onto a design, each creating the id it created before, and stops at the first
	 * one refused.
	 */
	public static Replay replay(Design design, List<DesignStep> steps, DesignToolContext context) {
		var current = design;
		for (var index = 0; index < steps.size(); index++) {
			var step = steps.get(index);
			var replaying = new DesignToolContext(context.templates(), context.addableTemplates(), context.frontStyles(),
					prefix -> Optional.ofNullable(step.getCreatedId()).orElseGet(() -> context.newId().apply(prefix)));
			var result = take(current, step.getTool(), step.getArguments(), replaying);
			if (result instanceof Refused refused) {
				return new Replay(current, index, index, refused);
			}
			current = ((Taken) result).design();
		}
		return new Replay(current, steps.size(), null, null);
	}
}
