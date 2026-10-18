package dev.daniellavoie.opencabinet.cutlist.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.drawing.domain.LengthFormat;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.model.NestingLayout;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Each nested sheet drawn as the app shows it: the sheet's length left to right, its first strip at
 * the top, every Part outlined, labelled when there is room, each cut numbered where it starts, and
 * the sheet's size dimensioned.
 */
final class NestingDrawings {
	/**
	 * Parts smaller than this keep their outline but not their label, as in the app.
	 */
	static final double LABEL_MIN_LENGTH_MM = 220;
	static final double LABEL_MIN_WIDTH_MM = 90;

	private NestingDrawings() {
	}

	static String layout(NestingLayout layout, DrawingLanguage language) {
		return layout == NestingLayout.CROSSCUT_FIRST ? language.pick("tronçonner d'abord", "crosscut first")
				: language.pick("refendre d'abord", "rip first");
	}

	static List<View> sheets(List<MaterialNesting> nesting, Units units, DrawingLanguage language) {
		var views = new ArrayList<View>();
		for (var material : nesting) {
			var sheets = material.getSheets();
			for (int index = 0; index < sheets.size(); index++) {
				var sheet = sheets.get(index);
				var length = material.getSheetLengthMm();
				var width = material.getSheetWidthMm();
				var shapes = new ArrayList<Shape>();
				shapes.add(Outline.box(0, 0, length, width, Stroke.OUTLINE));
				for (var placement : sheet.getPlacements()) {
					// Across is measured from the top edge, as on screen.
					var y = width - placement.getAcrossMm() - placement.getWidthMm();
					shapes.add(Outline.box(placement.getAlongMm(), y, placement.getLengthMm(), placement.getWidthMm(),
							Stroke.THIN));
					if (placement.getLengthMm() > LABEL_MIN_LENGTH_MM && placement.getWidthMm() > LABEL_MIN_WIDTH_MM) {
						shapes.add(new Label(new Point(placement.getAlongMm() + placement.getLengthMm() / 2,
								y + placement.getWidthMm() / 2),
								placement.getCabinetLabel() + " " + language.partRole(placement.getRole())));
					}
				}
				for (var cut : Optional.ofNullable(sheet.getCuts()).orElse(List.of())) {
					shapes.add(new Label(new Point(cut.getFromAlongMm(), width - cut.getFromAcrossMm()), cut.getNumber()));
				}
				shapes.add(new Dimension(new Point(length, 0), new Point(0, 0), 1, LengthFormat.format(length, units)));
				shapes.add(new Dimension(new Point(0, 0), new Point(0, width), 1, LengthFormat.format(width, units)));

				views.add(View.builder()
						.title(material.getMaterialName() + " — " + language.pick("panneau ", "sheet ") + (index + 1)
								+ " / " + sheets.size() + " — " + Math.round(sheet.getUtilization() * 100)
								+ language.pick(" % utilisé", "% used") + " — " + layout(material.getLayout(), language))
						.shapes(shapes)
						.build());
			}
		}
		return views;
	}
}
