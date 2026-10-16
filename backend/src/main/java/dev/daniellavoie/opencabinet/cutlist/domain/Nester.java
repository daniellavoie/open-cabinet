package dev.daniellavoie.opencabinet.cutlist.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestedSheet;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;

/**
 * Guillotine Nesting: each sheet is first ripped into strips across its width, then each strip is
 * cross-cut into Parts, so every layout can be cut on a panel saw. Grained Parts keep their length
 * along the sheet's grain; others may turn. Parts are placed largest first into the first strip
 * they fit (first-fit decreasing).
 */
final class Nester {

	private Nester() {
	}

	private static final class Strip {
		final double y;
		final double height;
		double usedLength;

		Strip(double y, double height) {
			this.y = y;
			this.height = height;
		}
	}

	private static final class Sheet {
		final List<Strip> strips = new ArrayList<>();
		final List<SheetPlacement> placements = new ArrayList<>();
		double usedWidth;
		double partArea;
	}

	private record Oriented(Part part, double alongLength, double acrossWidth, boolean rotated) {
	}

	static MaterialNesting nest(Material sheetMaterial, List<Part> parts, double kerfMm, double trimMm) {
		var usableLength = sheetMaterial.getLengthMm() - 2 * trimMm;
		var usableWidth = sheetMaterial.getWidthMm() - 2 * trimMm;

		var oversized = new ArrayList<Part>();
		var oriented = new ArrayList<Oriented>();
		for (var part : parts) {
			var orientation = orient(part, usableLength, usableWidth);
			if (orientation == null) {
				oversized.add(part);
			} else {
				oriented.add(orientation);
			}
		}

		oriented.sort(Comparator.comparingDouble(Oriented::acrossWidth)
				.thenComparingDouble(Oriented::alongLength)
				.reversed());

		var sheets = new ArrayList<Sheet>();
		for (var item : oriented) {
			place(item, sheets, usableLength, usableWidth, kerfMm, trimMm);
		}

		var sheetArea = sheetMaterial.getLengthMm() * sheetMaterial.getWidthMm();
		return MaterialNesting.builder()
				.materialId(sheetMaterial.getId())
				.materialName(sheetMaterial.getName())
				.sheetLengthMm(sheetMaterial.getLengthMm())
				.sheetWidthMm(sheetMaterial.getWidthMm())
				.grain(sheetMaterial.isGrain())
				.sheets(sheets.stream()
						.map(sheet -> NestedSheet.builder()
								.placements(List.copyOf(sheet.placements))
								.utilization(Math.round(sheet.partArea / sheetArea * 1000) / 1000d)
								.build())
						.toList())
				.oversizedParts(List.copyOf(oversized))
				.build();
	}

	/**
	 * Picks how a Part lies on the sheet, or null when it cannot fit at all. The long side goes
	 * along the sheet unless grain forbids turning.
	 */
	private static Oriented orient(Part part, double usableLength, double usableWidth) {
		var natural = new Oriented(part, part.getLengthMm(), part.getWidthMm(), false);
		if (part.isGrain()) {
			return fits(natural, usableLength, usableWidth) ? natural : null;
		}

		var turned = new Oriented(part, part.getWidthMm(), part.getLengthMm(), true);
		var preferred = part.getLengthMm() >= part.getWidthMm() ? natural : turned;
		var other = preferred == natural ? turned : natural;

		if (fits(preferred, usableLength, usableWidth)) {
			return preferred;
		}
		return fits(other, usableLength, usableWidth) ? other : null;
	}

	private static boolean fits(Oriented item, double usableLength, double usableWidth) {
		return item.alongLength() <= usableLength && item.acrossWidth() <= usableWidth;
	}

	private static void place(Oriented item, List<Sheet> sheets, double usableLength, double usableWidth,
			double kerfMm, double trimMm) {
		for (var sheet : sheets) {
			for (var strip : sheet.strips) {
				var start = strip.usedLength == 0 ? 0 : strip.usedLength + kerfMm;
				if (item.acrossWidth() <= strip.height && start + item.alongLength() <= usableLength) {
					put(sheet, strip, start, item, trimMm);
					return;
				}
			}
		}

		for (var sheet : sheets) {
			var y = sheet.usedWidth == 0 ? 0 : sheet.usedWidth + kerfMm;
			if (y + item.acrossWidth() <= usableWidth) {
				put(sheet, openStrip(sheet, y, item), 0, item, trimMm);
				return;
			}
		}

		var sheet = new Sheet();
		sheets.add(sheet);
		put(sheet, openStrip(sheet, 0, item), 0, item, trimMm);
	}

	private static Strip openStrip(Sheet sheet, double y, Oriented item) {
		var strip = new Strip(y, item.acrossWidth());
		sheet.strips.add(strip);
		sheet.usedWidth = y + item.acrossWidth();
		return strip;
	}

	private static void put(Sheet sheet, Strip strip, double x, Oriented item, double trimMm) {
		sheet.placements.add(SheetPlacement.builder()
				.cabinetLabel(item.part().getCabinetLabel())
				.role(item.part().getRole())
				.alongMm(trimMm + x)
				.acrossMm(trimMm + strip.y)
				.lengthMm(item.alongLength())
				.widthMm(item.acrossWidth())
				.rotated(item.rotated())
				.build());
		strip.usedLength = x + item.alongLength();
		sheet.partArea += item.alongLength() * item.acrossWidth();
	}
}
