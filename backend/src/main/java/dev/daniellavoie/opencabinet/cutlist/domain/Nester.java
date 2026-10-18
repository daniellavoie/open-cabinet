package dev.daniellavoie.opencabinet.cutlist.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestedSheet;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestingAlternative;
import dev.daniellavoie.opencabinet.project.domain.model.NestingLayout;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetCut;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;

/**
 * Guillotine Nesting, so every layout can be cut on a panel saw. A sheet is cut into strips (ripped
 * along its length, or crosscut across it, depending on the {@link NestingLayout}), each strip into
 * columns, and each column into the Parts stacked in it; a Part narrower than its column is trimmed.
 * Grained Parts keep their length along the sheet's grain; others may turn.
 *
 * <p>
 * Each layout is packed with a few Part orderings, each placing Parts first fit and best fit, and
 * keeps its best result: fewest sheets, then fewest cuts. No strip or column is narrower than the minimum offcut, so the saw can always hold
 * what it cuts.
 */
final class Nester {
	private static final double TOLERANCE_MM = 0.01;

	/**
	 * The saw's settings, from the Construction Standard.
	 */
	record Saw(double kerfMm, double trimMm, double minOffcutMm) {
	}

	/**
	 * A Part as it lies in a layout's frame: {@code along} the strips, {@code across} them.
	 * {@code rotated} is relative to the sheet: the Part's length runs across the sheet's length.
	 */
	private record Item(Part part, double along, double across, boolean rotated) {
	}

	private record Placed(Item item, double x, double y) {
	}

	private record Cut(String number, double x1, double y1, double x2, double y2) {
	}

	private static final class Column {
		final double x;
		final double width;
		final List<Placed> items = new ArrayList<>();
		double usedHeight;

		Column(double x, double width) {
			this.x = x;
			this.width = width;
		}
	}

	private static final class Strip {
		final double y;
		final double height;
		final List<Column> columns = new ArrayList<>();
		double usedLength;

		Strip(double y, double height) {
			this.y = y;
			this.height = height;
		}
	}

	private static final class Sheet {
		final List<Strip> strips = new ArrayList<>();
		double usedWidth;
		double partArea;
	}

	/**
	 * Sheets packed in one layout's frame, whose length runs along the strips.
	 */
	private record Packing(NestingLayout layout, double frameLength, double frameWidth, List<Sheet> sheets) {
		int cutCount() {
			return sheets.stream().mapToInt(sheet -> cuts(this, sheet).size()).sum();
		}
	}

	/**
	 * Where a Part goes among the places it fits: the first one, or the one it fills best.
	 */
	private enum Fit {
		FIRST, BEST
	}

	private static final List<Comparator<Item>> ORDERINGS = List.of(
			Comparator.comparingDouble(Item::across).thenComparingDouble(Item::along).reversed(),
			Comparator.comparingDouble(Item::along).thenComparingDouble(Item::across).reversed(),
			Comparator.<Item>comparingDouble(item -> item.along() * item.across()).reversed());

	private Nester() {
	}

	/**
	 * Nests the Parts in every layout and keeps {@code layout}'s result, or the one with the fewest
	 * sheets when {@code layout} is null.
	 */
	static MaterialNesting nest(Material sheetMaterial, List<Part> parts, Saw saw, NestingLayout layout) {
		var usableLength = sheetMaterial.getLengthMm() - 2 * saw.trimMm();
		var usableWidth = sheetMaterial.getWidthMm() - 2 * saw.trimMm();

		var oversized = new ArrayList<Part>();
		var fitting = new ArrayList<Part>();
		for (var part : parts) {
			(item(part, NestingLayout.RIP_FIRST, usableLength, usableWidth) == null ? oversized : fitting).add(part);
		}

		Map<NestingLayout, Packing> best = new EnumMap<>(NestingLayout.class);
		for (var candidate : NestingLayout.values()) {
			best.put(candidate, ORDERINGS.stream()
					.flatMap(ordering -> Arrays.stream(Fit.values())
							.map(fit -> pack(candidate, fitting, ordering, fit, usableLength, usableWidth, saw)))
					.min(Comparator.comparingInt((Packing packing) -> packing.sheets().size())
							.thenComparingInt(Packing::cutCount))
					.orElseThrow());
		}
		var chosen = layout != null ? best.get(layout)
				: best.values().stream()
						.min(Comparator.comparingInt((Packing packing) -> packing.sheets().size())
								.thenComparingInt(Packing::cutCount))
						.orElseThrow();

		var sheetArea = sheetMaterial.getLengthMm() * sheetMaterial.getWidthMm();
		return MaterialNesting.builder()
				.materialId(sheetMaterial.getId())
				.materialName(sheetMaterial.getName())
				.sheetLengthMm(sheetMaterial.getLengthMm())
				.sheetWidthMm(sheetMaterial.getWidthMm())
				.grain(sheetMaterial.isGrain())
				.layout(chosen.layout())
				.sheets(chosen.sheets().stream()
						.map(sheet -> NestedSheet.builder()
								.placements(placements(chosen, sheet, saw.trimMm()))
								.utilization(Math.round(sheet.partArea / sheetArea * 1000) / 1000d)
								.cuts(cuts(chosen, sheet).stream().map(cut -> sheetCut(chosen, cut, saw.trimMm())).toList())
								.build())
						.toList())
				.oversizedParts(List.copyOf(oversized))
				.alternatives(Arrays.stream(NestingLayout.values())
						.map(candidate -> {
							var packing = best.get(candidate);
							var area = packing.sheets().stream().mapToDouble(sheet -> sheet.partArea).sum();
							return NestingAlternative.builder()
									.layout(candidate)
									.sheets(packing.sheets().size())
									.cuts(packing.cutCount())
									.utilization(packing.sheets().isEmpty() ? 0
											: Math.round(area / (sheetArea * packing.sheets().size()) * 1000) / 1000d)
									.build();
						})
						.toList())
				.build();
	}

	private static Packing pack(NestingLayout layout, List<Part> parts, Comparator<Item> ordering, Fit fit,
			double usableLength, double usableWidth, Saw saw) {
		var frameLength = layout == NestingLayout.RIP_FIRST ? usableLength : usableWidth;
		var frameWidth = layout == NestingLayout.RIP_FIRST ? usableWidth : usableLength;
		var items = parts.stream().map(part -> item(part, layout, usableLength, usableWidth)).sorted(ordering).toList();

		var sheets = new ArrayList<Sheet>();
		for (var item : items) {
			place(item, sheets, frameLength, frameWidth, saw, fit);
		}
		return new Packing(layout, frameLength, frameWidth, sheets);
	}

	/**
	 * How a Part lies in a layout's frame, or null when it cannot fit the sheet at all. Grained Parts
	 * keep their length along the sheet; others lie with their long side along the strips when they
	 * fit that way.
	 */
	private static Item item(Part part, NestingLayout layout, double usableLength, double usableWidth) {
		var natural = frame(part, layout, part.getLengthMm(), part.getWidthMm(), false);
		if (part.isGrain()) {
			return fits(natural, layout, usableLength, usableWidth) ? natural : null;
		}

		var turned = frame(part, layout, part.getWidthMm(), part.getLengthMm(), true);
		var preferred = natural.along() >= natural.across() ? natural : turned;
		var other = preferred == natural ? turned : natural;
		if (fits(preferred, layout, usableLength, usableWidth)) {
			return preferred;
		}
		return fits(other, layout, usableLength, usableWidth) ? other : null;
	}

	/**
	 * A Part lying {@code alongSheet × acrossSheet} on the sheet, in the layout's frame.
	 */
	private static Item frame(Part part, NestingLayout layout, double alongSheet, double acrossSheet, boolean rotated) {
		return layout == NestingLayout.RIP_FIRST ? new Item(part, alongSheet, acrossSheet, rotated)
				: new Item(part, acrossSheet, alongSheet, rotated);
	}

	private static boolean fits(Item item, NestingLayout layout, double usableLength, double usableWidth) {
		var frameLength = layout == NestingLayout.RIP_FIRST ? usableLength : usableWidth;
		var frameWidth = layout == NestingLayout.RIP_FIRST ? usableWidth : usableLength;
		return item.along() <= frameLength + TOLERANCE_MM && item.across() <= frameWidth + TOLERANCE_MM;
	}

	/**
	 * Stacks the Part in a column it fits, else starts a column in a strip, else a strip on a sheet,
	 * else a new sheet. With {@link Fit#BEST}, the column or strip it leaves the least room in. New
	 * strips and columns are at least the minimum offcut wide.
	 */
	private static void place(Item item, List<Sheet> sheets, double frameLength, double frameWidth, Saw saw,
			Fit fit) {
		var kerf = saw.kerfMm();
		Sheet stackSheet = null;
		Column stackColumn = null;
		var stackY = 0d;
		var stackLeft = Double.MAX_VALUE;
		search: for (var sheet : sheets) {
			for (var strip : sheet.strips) {
				for (var column : strip.columns) {
					var y = column.usedHeight + kerf;
					var left = strip.height - y - item.across() + (column.width - item.along());
					if (item.along() <= column.width + TOLERANCE_MM && y + item.across() <= strip.height + TOLERANCE_MM
							&& left < stackLeft) {
						stackSheet = sheet;
						stackColumn = column;
						stackY = strip.y + y;
						stackLeft = left;
						if (fit == Fit.FIRST) {
							break search;
						}
					}
				}
			}
		}
		if (stackColumn != null) {
			stackColumn.items.add(new Placed(item, stackColumn.x, stackY));
			stackColumn.usedHeight = stackY - stripOf(stackSheet, stackColumn).y + item.across();
			stackSheet.partArea += item.along() * item.across();
			return;
		}

		Sheet columnSheet = null;
		Strip columnStrip = null;
		var columnLeft = Double.MAX_VALUE;
		search: for (var sheet : sheets) {
			for (var strip : sheet.strips) {
				var x = strip.usedLength == 0 ? 0 : strip.usedLength + kerf;
				var left = strip.height - item.across();
				if (item.across() <= strip.height + TOLERANCE_MM && x + item.along() <= frameLength + TOLERANCE_MM
						&& left < columnLeft) {
					columnSheet = sheet;
					columnStrip = strip;
					columnLeft = left;
					if (fit == Fit.FIRST) {
						break search;
					}
				}
			}
		}
		if (columnStrip != null) {
			var x = columnStrip.usedLength == 0 ? 0 : columnStrip.usedLength + kerf;
			openColumn(columnSheet, columnStrip, x, item, frameLength, saw);
			return;
		}

		for (var sheet : sheets) {
			var y = sheet.usedWidth == 0 ? 0 : sheet.usedWidth + kerf;
			if (y + item.across() <= frameWidth + TOLERANCE_MM) {
				openColumn(sheet, openStrip(sheet, y, item, frameWidth, saw), 0, item, frameLength, saw);
				return;
			}
		}

		var sheet = new Sheet();
		sheets.add(sheet);
		openColumn(sheet, openStrip(sheet, 0, item, frameWidth, saw), 0, item, frameLength, saw);
	}

	private static Strip stripOf(Sheet sheet, Column column) {
		return sheet.strips.stream().filter(strip -> strip.columns.contains(column)).findFirst().orElseThrow();
	}

	private static Strip openStrip(Sheet sheet, double y, Item item, double frameWidth, Saw saw) {
		var strip = new Strip(y, Math.min(Math.max(item.across(), saw.minOffcutMm()), frameWidth - y));
		sheet.strips.add(strip);
		sheet.usedWidth = y + strip.height;
		return strip;
	}

	private static void openColumn(Sheet sheet, Strip strip, double x, Item item, double frameLength, Saw saw) {
		var column = new Column(x, Math.min(Math.max(item.along(), saw.minOffcutMm()), frameLength - x));
		column.items.add(new Placed(item, x, strip.y));
		column.usedHeight = item.across();
		strip.columns.add(column);
		strip.usedLength = x + column.width;
		sheet.partArea += item.along() * item.across();
	}

	/**
	 * The cuts of a sheet in cutting order, in the frame: each strip off the sheet (1, 2…), each
	 * column off its strip (1.1, 1.2…), then in each column the Parts off each other and their trims
	 * (1.1.1…). A piece already at the sheet's or strip's edge needs no cut there.
	 */
	private static List<Cut> cuts(Packing packing, Sheet sheet) {
		var cuts = new ArrayList<Cut>();
		for (int s = 0; s < sheet.strips.size(); s++) {
			var strip = sheet.strips.get(s);
			var top = strip.y + strip.height;
			if (top < packing.frameWidth() - TOLERANCE_MM) {
				cuts.add(new Cut(Integer.toString(s + 1), 0, top, packing.frameLength(), top));
			}
			for (int c = 0; c < strip.columns.size(); c++) {
				var column = strip.columns.get(c);
				var end = column.x + column.width;
				if (end < packing.frameLength() - TOLERANCE_MM) {
					cuts.add(new Cut((s + 1) + "." + (c + 1), end, strip.y, end, top));
				}
				var number = 0;
				for (var placed : column.items) {
					var itemTop = placed.y() + placed.item().across();
					if (itemTop < top - TOLERANCE_MM) {
						cuts.add(new Cut((s + 1) + "." + (c + 1) + "." + (++number), column.x, itemTop, end, itemTop));
					}
					var itemEnd = column.x + placed.item().along();
					if (itemEnd < end - TOLERANCE_MM) {
						cuts.add(new Cut((s + 1) + "." + (c + 1) + "." + (++number), itemEnd, placed.y(), itemEnd, itemTop));
					}
				}
			}
		}
		return cuts;
	}

	private static List<SheetPlacement> placements(Packing packing, Sheet sheet, double trimMm) {
		var ripFirst = packing.layout() == NestingLayout.RIP_FIRST;
		return sheet.strips.stream()
				.flatMap(strip -> strip.columns.stream())
				.flatMap(column -> column.items.stream())
				.map(placed -> {
					var item = placed.item();
					return SheetPlacement.builder()
							.cabinetLabel(item.part().getCabinetLabel())
							.role(item.part().getRole())
							.alongMm(trimMm + (ripFirst ? placed.x() : placed.y()))
							.acrossMm(trimMm + (ripFirst ? placed.y() : placed.x()))
							.lengthMm(ripFirst ? item.along() : item.across())
							.widthMm(ripFirst ? item.across() : item.along())
							.rotated(item.rotated())
							.build();
				})
				.toList();
	}

	private static SheetCut sheetCut(Packing packing, Cut cut, double trimMm) {
		var ripFirst = packing.layout() == NestingLayout.RIP_FIRST;
		return SheetCut.builder()
				.number(cut.number())
				.fromAlongMm(trimMm + (ripFirst ? cut.x1() : cut.y1()))
				.fromAcrossMm(trimMm + (ripFirst ? cut.y1() : cut.x1()))
				.toAlongMm(trimMm + (ripFirst ? cut.x2() : cut.y2()))
				.toAcrossMm(trimMm + (ripFirst ? cut.y2() : cut.x2()))
				.build();
	}
}
