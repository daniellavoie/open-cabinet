package dev.daniellavoie.opencabinet.drawing.domain;

import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.horizontal;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.rect;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.text;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.vertical;

import java.util.ArrayList;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.common.domain.Lengths;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * A nested sheet as the saw operator sees it: the sheet with its length (and grain) across the page
 * and each Part placed on it, labelled with its Cabinet and length by width.
 */
public final class NestingDrawings {
	/**
	 * Parts narrower than this on the drawn sheet carry no label.
	 */
	static final double LABEL_MIN_MM = 120;

	private NestingDrawings() {
	}

	public static Drawing sheet(MaterialNesting nesting, int sheetIndex, Units units, Language language) {
		var sheet = nesting.getSheets().get(sheetIndex);
		var length = nesting.getSheetLengthMm();
		var width = nesting.getSheetWidthMm();
		var shapes = new ArrayList<Shape>();

		// The sheet's corner at the top left, as it lies on the saw: along runs right, across runs down.
		shapes.add(rect(0, -width, length, width, Stroke.OUTLINE, Fill.MEDIUM));
		for (var placement : sheet.getPlacements()) {
			var x = placement.getAlongMm();
			var y = -placement.getAcrossMm() - placement.getWidthMm();
			shapes.add(rect(x, y, placement.getLengthMm(), placement.getWidthMm(), Stroke.THIN, Fill.WHITE));
			if (placement.getLengthMm() >= LABEL_MIN_MM && placement.getWidthMm() >= LABEL_MIN_MM / 2) {
				shapes.add(text(x + placement.getLengthMm() / 2, y + placement.getWidthMm() / 2 + 25,
						placement.getCabinetLabel(), TextStyle.LABEL));
				shapes.add(text(x + placement.getLengthMm() / 2, y + placement.getWidthMm() / 2 - 35,
						Lengths.format(placement.getLengthMm(), units) + " × " + Lengths.format(placement.getWidthMm(), units),
						TextStyle.SMALL));
			}
		}
		shapes.add(horizontal(0, length, 0, 120, Lengths.format(length, units)
				+ (nesting.isGrain() ? language.pick("  (fil →)", "  (grain →)") : "")));
		shapes.add(vertical(0, -width, 0, 120, Lengths.format(width, units)));

		var title = nesting.getMaterialName() + " — " + language.pick("panneau ", "sheet ") + (sheetIndex + 1) + " / "
				+ nesting.getSheets().size() + " — " + Math.round(sheet.getUtilization() * 100) + " %";
		return new Drawing(title, shapes);
	}
}
