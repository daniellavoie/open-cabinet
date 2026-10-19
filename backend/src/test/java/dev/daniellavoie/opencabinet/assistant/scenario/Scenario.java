package dev.daniellavoie.opencabinet.assistant.scenario;

import java.util.List;
import java.util.Map;

import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * A Design Assistant scenario: a Project's design, what Members say (with photos, sketches or
 * PDFs from the scenario's folder), and what the resulting Proposal must look like. The same files
 * drive the on-demand runs against the real model and the replays of their recordings.
 */
public record Scenario(String name, String description, String language, Units units, String shopStyle, Design design,
		List<Message> messages, Expectations expect) {

	public record Message(String text, List<String> attachments) {
		public List<String> attachments() {
			return attachments == null ? List.of() : attachments;
		}
	}

	/**
	 * A count that must be at least {@code min} and at most {@code max}, each when set.
	 */
	public record Range(Integer min, Integer max) {
		boolean contains(int count) {
			return (min == null || count >= min) && (max == null || count <= max);
		}

		@Override
		public String toString() {
			return min != null && min.equals(max) ? String.valueOf(min)
					: (min == null ? "≤ " + max : max == null ? "≥ " + min : min + "–" + max);
		}
	}

	/**
	 * A Cabinet from {@code template} under an Opening of {@code opening} kind, on the same Wall.
	 */
	public record Under(String template, String opening) {
	}

	/**
	 * What the final design and Proposal must show; every check left out is not made. The design
	 * always has to pass the Construction Rules and the assistant always has to answer.
	 */
	public record Expectations(Range steps, Range walls, Range cabinets, Map<String, Range> templates,
			Map<CabinetType, Range> types, Map<String, Range> openings, Map<String, Range> appliances, List<Under> under,
			Range openQuestions, Range assumptions) {
	}
}
