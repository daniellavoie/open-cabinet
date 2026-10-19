package dev.daniellavoie.opencabinet.assistant.domain;

import java.util.List;
import java.util.Locale;

import dev.daniellavoie.opencabinet.project.domain.DesignGuidelines;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * What a Remark says to the Design Assistant: a sentence naming the Cabinets or items involved and
 * how to fix it, in the Shop's units.
 */
final class RemarkMessages {
	private RemarkMessages() {
	}

	static String describe(Remark remark, Units units) {
		var ids = remark.getIds() == null ? List.<String>of() : remark.getIds();
		var first = ids.isEmpty() ? "?" : ids.get(0);
		var second = ids.size() > 1 ? ids.get(1) : "?";
		var measure = length(remark.getMeasureMm(), units);
		var limit = length(remark.getLimitMm(), units);
		return switch (remark.getCode()) {
		case DesignGuidelines.GAP -> "Gap of " + measure + " between " + first + " and " + second
				+ ": close it with a filler if it is " + limit + " or less, with a cabinet otherwise.";
		case DesignGuidelines.GAP_AT_END -> "Sliver of " + measure + " between " + first
				+ " and the end of its run: close it with a filler, or extend the run.";
		case DesignGuidelines.FILLER_WIDTH -> "Filler " + first + " is " + measure + " wide: fillers are 1\" to 3\" (25 to 75 mm).";
		case DesignGuidelines.FILLER_MID_RUN -> "Filler " + first
				+ " sits in the middle of a run: fillers belong at a run's ends, against a wall, a corner or a tall unit.";
		case DesignGuidelines.WIDTH_STEP -> "Cabinet " + first + " is " + measure + " wide: use widths in steps of " + limit + ".";
		case DesignGuidelines.CORNER_NOT_BLIND -> "Base runs meet in a corner at " + String.join(" and ", ids)
				+ " without a blind corner cabinet: turn the corner with one, unless the Shop Style says otherwise.";
		case DesignGuidelines.CORNER_CLEARANCE -> first + " stands " + measure + " from the corner cabinet " + second
				+ ": leave at least " + limit + ", with a filler.";
		case DesignGuidelines.UPPER_CORNER_CLEARANCE -> "Wall cabinet " + first + " stands " + measure + " from the corner wall cabinet "
				+ second + ": stop one run short of the corner with at least " + limit + " of filler.";
		case DesignGuidelines.SINK_NOT_CENTRED -> "Sink base " + first + " is " + measure + " off the centre of window " + second
				+ ": centre it under the window.";
		case DesignGuidelines.DISHWASHER_NOT_BESIDE_SINK -> "Dishwasher " + first + " is not beside the sink base: put it right next to it.";
		case DesignGuidelines.DISHWASHER_IN_CORNER -> "Dishwasher " + first
				+ " is in a corner, against the run on the other wall: move it beside the sink, away from the corner.";
		case DesignGuidelines.LANDING_RANGE -> "Range " + first + " has " + measure
				+ " of counter on one side: give it at least 12\" (300 mm) on one side and 15\" (380 mm) on the other.";
		case DesignGuidelines.LANDING_SINK -> "Sink base " + first + " has " + measure
				+ " of counter on one side: give it at least 18\" (450 mm) on one side and 24\" (600 mm) on the other.";
		case DesignGuidelines.LANDING_FRIDGE -> "Fridge " + first + " has " + measure
				+ " of counter beside it: give it at least 15\" (380 mm) on one side, its handle side if known.";
		case DesignGuidelines.WORK_TRIANGLE -> "Work triangle: " + String.join(" to ", ids) + " is " + measure
				+ " (limit " + limit + "): keep each leg 4' to 9' (1.2 to 2.7 m) and the three under 26' (7.9 m).";
		case DesignGuidelines.WORK_TRIANGLE_BLOCKED -> "Tall unit " + first + " stands across the work triangle between "
				+ String.join(" and ", ids.subList(Math.min(1, ids.size()), ids.size())) + ": move it to a run's end.";
		case DesignGuidelines.UPPER_ALIGNMENT -> "Wall cabinet " + first + " does not line up with " + second
				+ " below it: line its sides up with the cabinets and appliances underneath.";
		case DesignGuidelines.UPPER_TOP_LINE -> "Wall cabinet " + first + "'s top is " + measure
				+ " off the others on its wall: give all wall cabinets on a wall one top line.";
		case DesignGuidelines.RANGE_CLEARANCE -> "Wall cabinet " + first + " leaves " + measure + " above range " + second
				+ ": leave at least " + limit + ", or put a hood there.";
		case DesignGuidelines.TALL_MID_RUN -> "Tall unit " + first + " cuts a counter in two: put tall units at a run's end.";
		case DesignGuidelines.HOOD_WITHOUT_RANGE -> "Hood " + first + " is over no range: put it over the range, or remove it.";
		case DesignGuidelines.HOOD_OVER_RANGE -> "Hood " + first + " is not centred over range " + second
				+ ", or is narrower: centre it and make it at least as wide.";
		default -> remark.getCode() + " at " + String.join(", ", ids);
		};
	}

	/**
	 * A length as the Shop reads it: inches to the eighth, or millimetres.
	 */
	static String length(Double millimetres, Units units) {
		if (millimetres == null) {
			return "?";
		}
		if (units == Units.METRIC) {
			return String.format(Locale.ROOT, "%.0f mm", millimetres);
		}
		var eighths = Math.round(millimetres / 25.4 * 8) / 8.0;
		return (eighths == Math.rint(eighths) ? String.format(Locale.ROOT, "%.0f", eighths)
				: String.format(Locale.ROOT, "%.3f", eighths).replaceAll("0+$", "")) + "\"";
	}
}
