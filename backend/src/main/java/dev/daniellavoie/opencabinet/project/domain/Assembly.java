package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.AssemblyPart;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Attachment;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Axis;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Banding;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Count;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Face;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.FrontStack;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRef;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.PartKind;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StackRow;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StandardValue;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Term;

/**
 * Builds a Cabinet from an assembled template (docs/adr/0006-template-is-an-assembly-of-parts.md):
 * resolves every Attachment at the Cabinet's size into boxes, repeats Parts, places dividers, lays
 * out front stacks and counts hardware. The same as {@code assembly.ts}, kept in step by
 * {@code template-assembly/cases.json}.
 */
public final class Assembly {
	static final double TOLERANCE_MM = 0.01;

	/**
	 * What a Cabinet brings to its template: its size, its per-Cabinet choices, the Construction
	 * Standard's values and the thickness of each material.
	 */
	public record Context(double width, double height, double depth, int shelfCount, List<Double> dividers,
			Map<StandardValue, Double> standard, Function<MaterialRef, Double> thickness) {
	}

	public record Box(double x0, double x1, double y0, double y1, double z0, double z1) {
		public double min(Axis axis) {
			return switch (axis) {
			case X -> x0;
			case Y -> y0;
			case Z -> z0;
			};
		}

		public double max(Axis axis) {
			return switch (axis) {
			case X -> x1;
			case Y -> y1;
			case Z -> z1;
			};
		}

		public double size(Axis axis) {
			return max(axis) - min(axis);
		}

		static Box of(Map<Axis, double[]> extents) {
			return new Box(extents.get(Axis.X)[0], extents.get(Axis.X)[1], extents.get(Axis.Y)[0],
					extents.get(Axis.Y)[1], extents.get(Axis.Z)[0], extents.get(Axis.Z)[1]);
		}
	}

	/**
	 * A Part of the built Cabinet. {@code source} is the template Part or front stack it comes from;
	 * {@code front} marks Fronts, which the Cabinet's made-or-ordered choice applies to.
	 */
	public record AssembledPart(String id, String source, PartRole role, String name, PartKind kind, boolean front,
			MaterialRef material, Box box, Axis thickness, Axis grain, Banding banding) {
	}

	public record AssembledHardware(MaterialRef material, int quantity) {
	}

	/**
	 * A template that cannot be built as drawn: {@code code} names the problem, {@code partId} the
	 * template Part or front stack it is about.
	 */
	public record Violation(String code, String partId) {
	}

	public static final String UNKNOWN_TARGET = "UNKNOWN_TARGET";
	public static final String WRONG_ATTACHMENTS = "WRONG_ATTACHMENTS";
	public static final String UNRESOLVED = "UNRESOLVED";

	public record Result(List<AssembledPart> parts, List<AssembledHardware> hardware, List<Violation> violations) {
	}

	private Assembly() {
	}

	public static Result assemble(TemplateAssembly assembly, Context context) {
		var violations = new ArrayList<Violation>();
		var definitions = new LinkedHashMap<String, AssemblyPart>();
		assembly.partsOrEmpty().forEach(part -> definitions.put(part.id(), part));

		for (var part : definitions.values()) {
			checkAttachments(part, part.repeat() != null, false, definitions, violations);
		}
		Optional.ofNullable(assembly.divider())
				.ifPresent(divider -> checkAttachments(divider, false, true, definitions, violations));
		if (!violations.isEmpty()) {
			return new Result(List.of(), List.of(), violations);
		}

		var extents = new HashMap<String, Map<Axis, double[]>>();
		definitions.keySet().forEach(id -> extents.put(id, new EnumMap<>(Axis.class)));
		boolean progress = true;
		while (progress) {
			progress = false;
			for (var part : definitions.values()) {
				for (var axis : Axis.values()) {
					if (!extents.get(part.id()).containsKey(axis)) {
						var resolved = resolve(part, axis, part.repeat() != null, extents, context);
						if (resolved != null) {
							extents.get(part.id()).put(axis, resolved);
							progress = true;
						}
					}
				}
			}
		}
		definitions.values().stream()
				.filter(part -> extents.get(part.id()).size() < Axis.values().length)
				.forEach(part -> violations.add(new Violation(UNRESOLVED, part.id())));
		if (!violations.isEmpty()) {
			return new Result(List.of(), List.of(), violations);
		}

		var dividerParts = new ArrayList<AssembledPart>();
		var divider = assembly.divider();
		if (divider != null) {
			var thickness = context.thickness().apply(divider.material());
			var dividers = Optional.ofNullable(context.dividers()).orElse(List.of());
			for (int index = 0; index < dividers.size(); index++) {
				var dividerExtents = new EnumMap<Axis, double[]>(Axis.class);
				dividerExtents.put(Axis.X, new double[] { dividers.get(index) - thickness / 2,
						dividers.get(index) + thickness / 2 });
				for (var axis : List.of(Axis.Y, Axis.Z)) {
					var resolved = resolve(divider, axis, false, extents, context);
					if (resolved == null) {
						violations.add(new Violation(UNRESOLVED, divider.id()));
						return new Result(List.of(), List.of(), violations);
					}
					dividerExtents.put(axis, resolved);
				}
				dividerParts.add(assembled(divider, divider.id() + "#" + (index + 1), Box.of(dividerExtents)));
			}
		}

		var parts = new ArrayList<AssembledPart>();
		for (var part : definitions.values()) {
			var box = Box.of(extents.get(part.id()));
			if (part.repeat() == null) {
				parts.add(assembled(part, part.id(), box));
			} else {
				parts.addAll(copies(part, box, count(part.repeat().count(), context),
						context.thickness().apply(part.material()), dividerParts));
			}
		}
		parts.addAll(dividerParts);

		for (var stack : assembly.frontStacksOrEmpty()) {
			var fronts = fronts(stack, extents, context);
			if (fronts == null) {
				violations.add(new Violation(UNRESOLVED, stack.id()));
			} else {
				parts.addAll(fronts);
			}
		}

		return new Result(parts, hardware(assembly, parts), violations);
	}

	/**
	 * On each axis a Part needs exactly two of: its low face, its high face, its size. A panel's
	 * size on its thickness axis is its material's thickness; a repeated Part attaches both faces of
	 * that axis. A divider is placed across x by the Cabinet, so its LEFT and RIGHT stay free.
	 */
	private static void checkAttachments(AssemblyPart part, boolean repeated, boolean divider,
			Map<String, AssemblyPart> definitions, List<Violation> violations) {
		var faces = Optional.ofNullable(part.faces()).orElse(Map.of());
		for (var entry : faces.entrySet()) {
			var attachment = entry.getValue();
			if (!Attachment.CABINET.equals(attachment.to())) {
				var target = definitions.get(attachment.to());
				if (target == null) {
					violations.add(new Violation(UNKNOWN_TARGET, part.id()));
					return;
				}
				if (target.repeat() != null || attachment.face().axis() != entry.getKey().axis()) {
					violations.add(new Violation(WRONG_ATTACHMENTS, part.id()));
					return;
				}
			}
		}

		var panel = part.kindOrPanel() == PartKind.PANEL;
		if (panel && part.thickness() == null) {
			violations.add(new Violation(WRONG_ATTACHMENTS, part.id()));
			return;
		}
		for (var axis : Axis.values()) {
			if (divider && axis == Axis.X) {
				if (faces.containsKey(Face.LEFT) || faces.containsKey(Face.RIGHT) || part.thickness() != Axis.X) {
					violations.add(new Violation(WRONG_ATTACHMENTS, part.id()));
					return;
				}
				continue;
			}
			var given = (faces.containsKey(Face.of(axis, false)) ? 1 : 0) + (faces.containsKey(Face.of(axis, true)) ? 1 : 0);
			var sized = sizes(part).containsKey(axis) || (panel && axis == part.thickness() && !repeated);
			if (repeated && axis == part.thickness() ? given != 2 : given + (sized ? 1 : 0) != 2) {
				violations.add(new Violation(WRONG_ATTACHMENTS, part.id()));
				return;
			}
		}
	}

	/**
	 * A Part's low and high ends on an axis, once what it is attached to is known; null until then.
	 */
	private static double[] resolve(AssemblyPart part, Axis axis, boolean repeated,
			Map<String, Map<Axis, double[]>> extents, Context context) {
		var faces = Optional.ofNullable(part.faces()).orElse(Map.of());
		var low = faces.get(Face.of(axis, false));
		var high = faces.get(Face.of(axis, true));
		var lowValue = low == null ? null : position(low, extents, context);
		var highValue = high == null ? null : position(high, extents, context);
		if ((low != null && lowValue == null) || (high != null && highValue == null)) {
			return null;
		}
		if (repeated && axis == part.thickness()) {
			return new double[] { lowValue, highValue };
		}
		if (lowValue != null && highValue != null) {
			return new double[] { lowValue, highValue };
		}
		var size = size(part, axis, context);
		return lowValue != null ? new double[] { lowValue, lowValue + size } : new double[] { highValue - size, highValue };
	}

	private static double size(AssemblyPart part, Axis axis, Context context) {
		var terms = sizes(part).get(axis);
		return terms != null ? sum(terms, context) : context.thickness().apply(part.material());
	}

	private static Map<Axis, List<Term>> sizes(AssemblyPart part) {
		return Optional.ofNullable(part.sizes()).orElse(Map.of());
	}

	/**
	 * Where an attached face lies: the target face's position plus the offset; null while the target
	 * is not placed on that axis yet.
	 */
	private static Double position(Attachment attachment, Map<String, Map<Axis, double[]>> extents, Context context) {
		var face = attachment.face();
		double base;
		if (Attachment.CABINET.equals(attachment.to())) {
			base = !face.max() ? 0 : switch (face.axis()) {
			case X -> context.width();
			case Y -> context.height();
			case Z -> context.depth();
			};
		} else {
			var target = Optional.ofNullable(extents.get(attachment.to())).map(axes -> axes.get(face.axis()));
			if (target.isEmpty()) {
				return null;
			}
			base = face.max() ? target.get()[1] : target.get()[0];
		}
		return base + sum(attachment.offsetOrEmpty(), context);
	}

	static double sum(List<Term> terms, Context context) {
		double total = 0;
		for (var term : terms) {
			double value;
			if (term.mm() != null) {
				value = term.mm();
			} else if (term.standard() != null) {
				value = Optional.ofNullable(context.standard().get(term.standard())).orElse(0d);
			} else if (term.thickness() != null) {
				value = context.thickness().apply(term.thickness());
			} else {
				value = 0;
			}
			total += value * term.effectiveFactor();
		}
		return total;
	}

	private static int count(Count count, Context context) {
		return switch (count) {
		case SHELVES -> Math.max(0, context.shelfCount());
		};
	}

	/**
	 * {@code count} copies of a repeated Part, spaced evenly in its span on its thickness axis. Dividers
	 * inside its span across x split it into sections, each with {@code count} copies, as a starter
	 * template's shelves are.
	 */
	private static List<AssembledPart> copies(AssemblyPart part, Box span, int count, double thickness,
			List<AssembledPart> dividers) {
		var sections = new ArrayList<double[]>();
		var start = span.x0();
		var sorted = dividers.stream().sorted((first, second) -> Double.compare(first.box().x0(), second.box().x0()))
				.toList();
		for (var divider : sorted) {
			if (divider.box().x0() > start + TOLERANCE_MM && divider.box().x1() < span.x1() - TOLERANCE_MM) {
				sections.add(new double[] { start, divider.box().x0() });
				start = divider.box().x1();
			}
		}
		sections.add(new double[] { start, span.x1() });

		var axis = part.thickness();
		var copies = new ArrayList<AssembledPart>();
		for (int section = 0; section < sections.size(); section++) {
			var area = with(span, Axis.X, sections.get(section)[0], sections.get(section)[1]);
			var prefix = part.id() + "#" + (sections.size() > 1 ? (section + 1) + "." : "");
			var gap = (area.size(axis) - count * thickness) / (count + 1);
			for (int index = 0; index < count; index++) {
				var low = area.min(axis) + gap * (index + 1) + thickness * index;
				copies.add(assembled(part, prefix + (index + 1), with(area, axis, low, low + thickness)));
			}
		}
		return copies;
	}

	private static Box with(Box box, Axis axis, double low, double high) {
		return switch (axis) {
		case X -> new Box(low, high, box.y0(), box.y1(), box.z0(), box.z1());
		case Y -> new Box(box.x0(), box.x1(), low, high, box.z0(), box.z1());
		case Z -> new Box(box.x0(), box.x1(), box.y0(), box.y1(), low, high);
		};
	}

	private static AssembledPart assembled(AssemblyPart part, String id, Box box) {
		var thickness = part.thickness();
		return new AssembledPart(id, part.id(), part.role(), part.name(), part.kindOrPanel(), false, part.material(),
				box, thickness, grain(part.grain(), thickness), banding(part.banding(), part.kindOrPanel(), thickness));
	}

	/**
	 * A panel's in-plane axis that is not its grain.
	 */
	public static Axis otherAxis(Axis thickness, Axis grain) {
		for (var axis : Axis.values()) {
			if (axis != thickness && axis != grain) {
				return axis;
			}
		}
		throw new IllegalArgumentException("thickness and grain must differ");
	}

	/**
	 * Grain runs up unless the Part lies flat, then along x.
	 */
	static Axis grain(Axis grain, Axis thickness) {
		if (grain != null && grain != thickness) {
			return grain;
		}
		return thickness == Axis.Y ? Axis.X : Axis.Y;
	}

	/**
	 * A panel's front edge gets the carcass banding unless the template says otherwise.
	 */
	private static Banding banding(Banding banding, PartKind kind, Axis thickness) {
		if (banding != null || kind != PartKind.PANEL || thickness == Axis.Z) {
			return banding;
		}
		return new Banding(List.of(Face.FRONT), new MaterialRef(MaterialRole.CARCASS_BANDING, null));
	}

	/**
	 * A front stack's Fronts, sized like a starter template's front rows: rows from the top with the
	 * Standard's door gap, doors split in two leaves above {@code splitAboveMm}. Null until its
	 * area can be placed.
	 */
	private static List<AssembledPart> fronts(FrontStack stack, Map<String, Map<Axis, double[]>> extents,
			Context context) {
		var faces = Optional.ofNullable(stack.faces()).orElse(Map.of());
		var positions = new EnumMap<Face, Double>(Face.class);
		for (var face : List.of(Face.LEFT, Face.RIGHT, Face.BOTTOM, Face.TOP, Face.BACK)) {
			var attachment = faces.get(face);
			var position = attachment == null ? null : position(attachment, extents, context);
			if (position == null) {
				return null;
			}
			positions.put(face, position);
		}

		var gap = Optional.ofNullable(context.standard().get(StandardValue.DOOR_GAP)).orElse(0d);
		var left = positions.get(Face.LEFT);
		var width = positions.get(Face.RIGHT) - left;
		var bottom = positions.get(Face.BOTTOM);
		var back = positions.get(Face.BACK);
		var thickness = context.thickness().apply(stack.material());
		var banding = new Banding(List.of(Face.LEFT, Face.RIGHT, Face.BOTTOM, Face.TOP),
				new MaterialRef(MaterialRole.FRONT_BANDING, null));

		var rows = Optional.ofNullable(stack.rows()).orElse(List.of()).stream().map(StackRow::frontRow).toList();
		var fronts = new ArrayList<AssembledPart>();
		var sized = FrontLayout.rows(rows, positions.get(Face.TOP) - bottom, gap);
		for (int index = 0; index < sized.size(); index++) {
			var row = sized.get(index);
			var y0 = bottom + row.bottomMm();
			var y1 = y0 + row.heightMm();
			var id = stack.id() + "." + (index + 1);
			switch (row.kind()) {
			case DOOR -> {
				var leaves = stack.splitAboveMm() != null && width > stack.splitAboveMm() ? 2 : 1;
				var leafWidth = (width - gap * leaves) / leaves;
				for (int leaf = 0; leaf < leaves; leaf++) {
					var x0 = left + gap / 2 + leaf * (leafWidth + gap);
					fronts.add(new AssembledPart(id + "." + (leaf + 1), stack.id(), PartRole.DOOR, null, PartKind.PANEL,
							true, stack.material(), new Box(x0, x0 + leafWidth, y0, y1, back, back + thickness), Axis.Z,
							Axis.Y, banding));
				}
			}
			case DRAWER, FALSE_FRONT -> fronts.add(new AssembledPart(id, stack.id(),
					row.kind() == FrontKind.DRAWER ? PartRole.DRAWER_FRONT : PartRole.FALSE_FRONT, null, PartKind.PANEL,
					true, stack.material(), new Box(left + gap / 2, left + width - gap / 2, y0, y1, back, back + thickness),
					Axis.Z, Axis.X, banding));
			case OPENING -> {
			}
			}
		}
		return fronts;
	}

	/**
	 * Each hardware item counted on its hosts, totalled by material.
	 */
	private static List<AssembledHardware> hardware(TemplateAssembly assembly, List<AssembledPart> parts) {
		var totals = new LinkedHashMap<MaterialRef, Integer>();
		for (var item : assembly.hardwareOrEmpty()) {
			var hosts = item.on() == null ? null
					: parts.stream()
							.filter(part -> item.on().equals(part.source()))
							.filter(part -> item.frontKind() == null || part.role() == role(item.frontKind()))
							.toList();
			var quantity = item.quantity();
			int total;
			if (hosts == null) {
				total = Optional.ofNullable(quantity.fixed()).orElse(0);
			} else {
				total = hosts.stream().mapToInt(host -> {
					if (quantity.fixed() != null) {
						return quantity.fixed();
					}
					var axis = Optional.ofNullable(quantity.axis()).orElse(Axis.Y);
					var byLength = (int) Math.ceil(host.box().size(axis) / quantity.perMm() - TOLERANCE_MM);
					return Math.max(byLength, Optional.ofNullable(quantity.minimum()).orElse(0));
				}).sum();
			}
			if (total > 0) {
				totals.merge(item.material(), total, Integer::sum);
			}
		}
		return totals.entrySet().stream().map(entry -> new AssembledHardware(entry.getKey(), entry.getValue())).toList();
	}

	private static PartRole role(FrontKind kind) {
		return switch (kind) {
		case DOOR -> PartRole.DOOR;
		case DRAWER -> PartRole.DRAWER_FRONT;
		case FALSE_FRONT -> PartRole.FALSE_FRONT;
		case OPENING -> null;
		};
	}

	/**
	 * Every template Part or front stack named in the assembly, to catch duplicate names.
	 */
	static List<String> names(TemplateAssembly assembly) {
		var names = new ArrayList<String>();
		assembly.partsOrEmpty().forEach(part -> names.add(part.id()));
		assembly.frontStacksOrEmpty().forEach(stack -> names.add(stack.id()));
		Optional.ofNullable(assembly.divider()).ifPresent(divider -> names.add(divider.id()));
		return names;
	}

	static boolean unique(List<String> names) {
		return new HashSet<>(names).size() == names.size();
	}
}
