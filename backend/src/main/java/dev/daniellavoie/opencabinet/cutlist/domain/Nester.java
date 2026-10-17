package dev.daniellavoie.opencabinet.cutlist.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestedSheet;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingStrategy;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingStrategy.Order;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingStrategy.Orientation;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingStrategy.StripDirection;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;

/**
 * Guillotine Nesting in three stages, so every layout can be cut on a panel saw: each sheet is cut
 * into strips, each strip into stacks, and each stack into Parts piled across the strip. Grained
 * Parts keep their length along the sheet's grain; others may turn. Parts go first-fit into the
 * sheets already opened before a new sheet is started.
 *
 * <p>
 * Several strategies are tried (strip direction, order, how Parts without grain lie); the layout
 * with the fewest sheets wins, then the one leaving the largest clean offcut on its last sheet.
 */
final class Nester {
	/**
	 * Other layouts kept to compare with the chosen one.
	 */
	static final int ALTERNATIVES = 3;

	private Nester() {
	}

	/**
	 * Lengths in layout space: {@code u} runs along the strips, {@code v} across them.
	 */
	private record Item(Part part, double u, double v, boolean rotated) {
	}

	private static final class Stack {
		final double u;
		final double length;
		double usedV;

		Stack(double u, double length, double usedV) {
			this.u = u;
			this.length = length;
			this.usedV = usedV;
		}
	}

	private static final class Strip {
		final double v;
		final double height;
		final List<Stack> stacks = new ArrayList<>();
		double usedU;

		Strip(double v, double height) {
			this.v = v;
			this.height = height;
		}
	}

	private static final class Sheet {
		final List<Strip> strips = new ArrayList<>();
		final List<SheetPlacement> placements = new ArrayList<>();
		double usedV;
		double partArea;
	}

	private record Layout(NestingStrategy strategy, List<Sheet> sheets, double lastOffcut) {
	}

	static MaterialNesting nest(Material sheetMaterial, List<Part> parts, double kerfMm, double trimMm) {
		var usableLength = sheetMaterial.getLengthMm() - 2 * trimMm;
		var usableWidth = sheetMaterial.getWidthMm() - 2 * trimMm;

		var oversized = parts.stream().filter(part -> !fitsSomehow(part, usableLength, usableWidth)).toList();
		var placeable = parts.stream().filter(part -> fitsSomehow(part, usableLength, usableWidth)).toList();

		var layouts = new ArrayList<Layout>();
		for (var direction : StripDirection.values()) {
			for (var order : Order.values()) {
				for (var orientation : Orientation.values()) {
					var strategy = NestingStrategy.builder().stripDirection(direction).order(order)
							.orientation(orientation).build();
					layouts.add(layout(strategy, placeable, usableLength, usableWidth, kerfMm, trimMm));
				}
			}
		}
		layouts.sort(Comparator.<Layout>comparingInt(layout -> layout.sheets().size())
				.thenComparing(Comparator.comparingDouble(Layout::lastOffcut).reversed()));

		// Keep distinct alternatives: the same sheet count and offcut make the same choice for the Shop.
		var seen = new LinkedHashSet<List<Double>>();
		var distinct = layouts.stream()
				.filter(layout -> seen.add(List.of((double) layout.sheets().size(), Math.rint(layout.lastOffcut()))))
				.toList();

		var best = distinct.get(0);
		return result(sheetMaterial, best, oversized).toBuilder()
				.alternatives(distinct.stream().skip(1).limit(ALTERNATIVES)
						.map(layout -> result(sheetMaterial, layout, oversized))
						.toList())
				.build();
	}

	private static MaterialNesting result(Material sheetMaterial, Layout layout, List<Part> oversized) {
		var sheetArea = sheetMaterial.getLengthMm() * sheetMaterial.getWidthMm();
		return MaterialNesting.builder()
				.materialId(sheetMaterial.getId())
				.materialName(sheetMaterial.getName())
				.sheetLengthMm(sheetMaterial.getLengthMm())
				.sheetWidthMm(sheetMaterial.getWidthMm())
				.grain(sheetMaterial.isGrain())
				.sheets(layout.sheets().stream()
						.map(sheet -> NestedSheet.builder()
								.placements(List.copyOf(sheet.placements))
								.utilization(Math.round(sheet.partArea / sheetArea * 1000) / 1000d)
								.build())
						.toList())
				.oversizedParts(List.copyOf(oversized))
				.strategy(layout.strategy())
				.alternatives(List.of())
				.build();
	}

	private static Layout layout(NestingStrategy strategy, List<Part> parts, double usableLength,
			double usableWidth, double kerfMm, double trimMm) {
		var along = strategy.getStripDirection() == StripDirection.ALONG;
		var maxU = along ? usableLength : usableWidth;
		var maxV = along ? usableWidth : usableLength;

		var items = new ArrayList<Item>();
		for (var part : parts) {
			var orientation = orient(part, usableLength, usableWidth, strategy.getOrientation());
			// orientation[0] lies along the sheet, orientation[1] across it.
			items.add(along ? new Item(part, orientation[0], orientation[1], orientation[2] > 0)
					: new Item(part, orientation[1], orientation[0], orientation[2] > 0));
		}
		items.sort(switch (strategy.getOrder()) {
		case WIDTH -> Comparator.comparingDouble(Item::v).thenComparingDouble(Item::u).reversed();
		case LENGTH -> Comparator.comparingDouble(Item::u).thenComparingDouble(Item::v).reversed();
		case AREA -> Comparator.<Item>comparingDouble(item -> item.u() * item.v()).reversed();
		});

		var sheets = new ArrayList<Sheet>();
		for (var item : items) {
			place(item, sheets, maxU, maxV, kerfMm, trimMm, along);
		}

		var lastOffcut = sheets.isEmpty() ? 0
				: Math.max(0, maxV - sheets.get(sheets.size() - 1).usedV - kerfMm) * maxU;
		return new Layout(strategy, sheets, lastOffcut);
	}

	/**
	 * How a Part lies on the sheet as {along, across, turned (1) or not (0)}, assuming it fits one way.
	 */
	private static double[] orient(Part part, double usableLength, double usableWidth, Orientation orientation) {
		var natural = new double[] { part.getLengthMm(), part.getWidthMm(), 0 };
		if (part.isGrain()) {
			return natural;
		}
		var turned = new double[] { part.getWidthMm(), part.getLengthMm(), 1 };
		var longAlong = part.getLengthMm() >= part.getWidthMm() ? natural : turned;
		var longAcross = longAlong == natural ? turned : natural;
		var preferred = orientation == Orientation.LONG_ALONG ? longAlong : longAcross;
		var other = preferred == natural ? turned : natural;
		return fits(preferred, usableLength, usableWidth) ? preferred : other;
	}

	private static boolean fitsSomehow(Part part, double usableLength, double usableWidth) {
		var natural = new double[] { part.getLengthMm(), part.getWidthMm() };
		if (part.isGrain()) {
			return fits(natural, usableLength, usableWidth);
		}
		return fits(natural, usableLength, usableWidth)
				|| fits(new double[] { part.getWidthMm(), part.getLengthMm() }, usableLength, usableWidth);
	}

	private static boolean fits(double[] orientation, double usableLength, double usableWidth) {
		return orientation[0] <= usableLength && orientation[1] <= usableWidth;
	}

	private static void place(Item item, List<Sheet> sheets, double maxU, double maxV, double kerfMm, double trimMm,
			boolean along) {
		// A stack in a strip already cut, with room above for this Part.
		for (var sheet : sheets) {
			for (var strip : sheet.strips) {
				for (var stack : strip.stacks) {
					if (item.u() <= stack.length && stack.usedV + kerfMm + item.v() <= strip.height) {
						put(sheet, stack.u, strip.v + stack.usedV + kerfMm, item, trimMm, along);
						stack.usedV += kerfMm + item.v();
						return;
					}
				}
			}
		}

		// A new stack at the end of a strip tall enough.
		for (var sheet : sheets) {
			for (var strip : sheet.strips) {
				var start = strip.usedU == 0 ? 0 : strip.usedU + kerfMm;
				if (item.v() <= strip.height && start + item.u() <= maxU) {
					strip.stacks.add(new Stack(start, item.u(), item.v()));
					strip.usedU = start + item.u();
					put(sheet, start, strip.v, item, trimMm, along);
					return;
				}
			}
		}

		// A new strip on a sheet already opened, then a new sheet.
		for (var sheet : sheets) {
			var v = sheet.usedV == 0 ? 0 : sheet.usedV + kerfMm;
			if (v + item.v() <= maxV) {
				openStrip(sheet, v, item, trimMm, along);
				return;
			}
		}
		var sheet = new Sheet();
		sheets.add(sheet);
		openStrip(sheet, 0, item, trimMm, along);
	}

	private static void openStrip(Sheet sheet, double v, Item item, double trimMm, boolean along) {
		var strip = new Strip(v, item.v());
		strip.stacks.add(new Stack(0, item.u(), item.v()));
		strip.usedU = item.u();
		sheet.strips.add(strip);
		sheet.usedV = v + item.v();
		put(sheet, 0, v, item, trimMm, along);
	}

	private static void put(Sheet sheet, double u, double v, Item item, double trimMm, boolean along) {
		sheet.placements.add(SheetPlacement.builder()
				.cabinetLabel(item.part().getCabinetLabel())
				.role(item.part().getRole())
				.alongMm(trimMm + (along ? u : v))
				.acrossMm(trimMm + (along ? v : u))
				.lengthMm(along ? item.u() : item.v())
				.widthMm(along ? item.v() : item.u())
				.rotated(item.rotated())
				.build());
		sheet.partArea += item.u() * item.v();
	}
}
