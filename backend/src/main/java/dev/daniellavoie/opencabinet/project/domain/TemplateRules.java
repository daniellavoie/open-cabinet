package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dev.daniellavoie.opencabinet.project.domain.Assembly.AssembledPart;
import dev.daniellavoie.opencabinet.project.domain.Assembly.Box;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Axis;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRef;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.PartKind;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StandardValue;

/**
 * The rules an assembled template must pass before it is published (ADR 0006), at its minimum,
 * default and maximum size with as many shelves as it allows: it can be built, every Part has a
 * size, no two Parts overlap, every panel fits its sheet and every Part touches another. The same as
 * {@code template-rules.ts}, kept in step by {@code template-assembly/rules.json}.
 */
public final class TemplateRules {
	public static final String DUPLICATE_NAME = "DUPLICATE_NAME";
	public static final String NON_POSITIVE_SIZE = "NON_POSITIVE_SIZE";
	public static final String PARTS_OVERLAP = "PARTS_OVERLAP";
	public static final String TOO_BIG_FOR_SHEET = "TOO_BIG_FOR_SHEET";
	public static final String NOT_TOUCHING = "NOT_TOUCHING";

	public enum Size {
		MINIMUM, DEFAULT, MAXIMUM
	}

	/**
	 * A sheet material's size and whether it has a grain.
	 */
	public record Sheet(double lengthMm, double widthMm, boolean grain) {
	}

	/**
	 * What the Shop brings: its Construction Standard values, each material's thickness and, for
	 * sheet materials, their sheet; {@code sheet} answers null for materials that are not sheets.
	 */
	public record Shop(Map<StandardValue, Double> standard, Function<MaterialRef, Double> thickness,
			Function<MaterialRef, Sheet> sheet, double trimMm) {
	}

	public record Violation(String code, String partId, Size size) {
	}

	private TemplateRules() {
	}

	public static List<Violation> check(CabinetTemplate template, Shop shop) {
		var assembly = template.getAssembly();
		if (assembly == null) {
			return List.of();
		}
		if (!Assembly.unique(Assembly.names(assembly))) {
			return List.of(new Violation(DUPLICATE_NAME, null, null));
		}

		var violations = new LinkedHashSet<Violation>();
		var seen = new LinkedHashSet<String>();
		for (var size : Size.values()) {
			var context = new Assembly.Context(
					pick(size, template.getMinWidthMm(), template.getDefaultWidthMm(), template.getMaxWidthMm()),
					pick(size, template.getMinHeightMm(), template.getDefaultHeightMm(), template.getMaxHeightMm()),
					pick(size, template.getMinDepthMm(), template.getDefaultDepthMm(), template.getMaxDepthMm()),
					template.getMaxShelfCount(), List.of(), shop.standard(), shop.thickness(), false);
			var result = Assembly.assemble(assembly, context);
			var found = new ArrayList<Violation>();
			result.violations().forEach(violation -> found.add(new Violation(violation.code(), violation.partId(), size)));
			found.addAll(geometry(result.parts(), shop, size));
			for (var violation : found) {
				// Each problem once, at the first size it shows.
				if (seen.add(violation.code() + "/" + violation.partId())) {
					violations.add(violation);
				}
			}
		}
		return List.copyOf(violations);
	}

	private static double pick(Size size, double minimum, double fallback, double maximum) {
		return switch (size) {
		case MINIMUM -> minimum;
		case DEFAULT -> fallback;
		case MAXIMUM -> maximum;
		};
	}

	private static List<Violation> geometry(List<AssembledPart> parts, Shop shop, Size size) {
		var violations = new ArrayList<Violation>();
		for (var part : parts) {
			for (var axis : Axis.values()) {
				if (part.box().size(axis) <= Assembly.TOLERANCE_MM) {
					violations.add(new Violation(NON_POSITIVE_SIZE, part.source(), size));
					break;
				}
			}
		}

		for (int second = 0; second < parts.size(); second++) {
			for (int first = 0; first < second; first++) {
				if (overlap(parts.get(first).box(), parts.get(second).box())) {
					violations.add(new Violation(PARTS_OVERLAP, parts.get(second).source(), size));
				}
			}
		}

		for (var part : parts) {
			if (part.kind() == PartKind.PANEL && !fitsSheet(part, shop)) {
				violations.add(new Violation(TOO_BIG_FOR_SHEET, part.source(), size));
			}
		}

		if (parts.size() > 1) {
			for (var part : parts) {
				if (parts.stream().noneMatch(other -> other != part && touch(part.box(), other.box()))) {
					violations.add(new Violation(NOT_TOUCHING, part.source(), size));
				}
			}
		}
		return violations;
	}

	static boolean overlap(Box first, Box second) {
		for (var axis : Axis.values()) {
			var shared = Math.min(first.max(axis), second.max(axis)) - Math.max(first.min(axis), second.min(axis));
			if (shared <= Assembly.TOLERANCE_MM) {
				return false;
			}
		}
		return true;
	}

	static boolean touch(Box first, Box second) {
		for (var axis : Axis.values()) {
			if (first.min(axis) > second.max(axis) + Assembly.TOLERANCE_MM
					|| second.min(axis) > first.max(axis) + Assembly.TOLERANCE_MM) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether a panel can be cut from its sheet inside the trim: along the grain for grained sheets,
	 * either way otherwise. Unknown sheets pass.
	 */
	private static boolean fitsSheet(AssembledPart part, Shop shop) {
		var sheet = shop.sheet().apply(part.material());
		if (sheet == null) {
			return true;
		}
		var grain = part.grain();
		var other = Assembly.otherAxis(part.thickness(), grain);
		var along = part.box().size(grain);
		var across = part.box().size(other);
		var length = sheet.lengthMm() - 2 * shop.trimMm();
		var width = sheet.widthMm() - 2 * shop.trimMm();
		var fits = along <= length + Assembly.TOLERANCE_MM && across <= width + Assembly.TOLERANCE_MM;
		var turned = across <= length + Assembly.TOLERANCE_MM && along <= width + Assembly.TOLERANCE_MM;
		return sheet.grain() ? fits : fits || turned;
	}
}
