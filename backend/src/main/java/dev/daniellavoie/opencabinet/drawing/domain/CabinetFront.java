package dev.daniellavoie.opencabinet.drawing.domain;

import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.line;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.rect;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.project.domain.FrontLayout;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.Side;

/**
 * A Cabinet seen straight on, its left side at {@code x} and its bottom at {@code y}: toe kick,
 * box, Fronts with pulls, blind panel, finished ends and crown; dividers and shelves dashed behind
 * the doors when asked.
 */
final class CabinetFront {
	static final double FINISHED_END_MM = 19;
	static final double CROWN_HEIGHT_MM = 76.2;
	static final double PANEL_MM = 19;
	private static final double PULL_MM = 100;
	private static final double PULL_INSET_MM = 40;

	private CabinetFront() {
	}

	static void draw(List<Shape> shapes, Cabinet cabinet, CabinetTemplate template, double x, double y,
			DrawingContext context, boolean interior) {
		var width = cabinet.getWidthMm();
		var floor = template.getType().standsOnFloor();
		var boxBottom = floor ? context.toeKickHeightMm() : 0;
		var boxHeight = cabinet.getHeightMm() - boxBottom;
		var gap = context.frontGapMm();

		if (floor && boxBottom > 0) {
			shapes.add(rect(x, y, width, boxBottom, Stroke.THIN, Fill.MEDIUM));
		}
		shapes.add(rect(x, y + boxBottom, width, boxHeight, Stroke.OUTLINE, Fill.WHITE));

		var blind = template.getBlindWidthMm();
		var blindLeft = cabinet.effectiveBlindSide() == Side.LEFT;
		var frontWidth = FrontLayout.frontWidth(template, width);
		var frontLeft = x + (blind > 0 && blindLeft ? blind : 0);
		var boxY = y + boxBottom;

		for (var row : FrontLayout.rows(template, boxHeight, gap)) {
			var rowY = boxY + row.bottomMm();
			if (row.kind() == FrontKind.DOOR) {
				var leaves = FrontLayout.doorLeaves(frontWidth);
				var leafWidth = (frontWidth - gap * leaves) / leaves;
				for (int leaf = 0; leaf < leaves; leaf++) {
					var leafX = frontLeft + gap / 2 + leaf * (leafWidth + gap);
					shapes.add(rect(leafX, rowY, leafWidth, row.heightMm(), Stroke.THIN, Fill.LIGHT));
					// The pull sits on the opening edge: towards the middle of a pair, else away from the hinges.
					var pullX = leaves == 2 ? (leaf == 0 ? leafX + leafWidth - PULL_INSET_MM : leafX + PULL_INSET_MM)
							: leafX + leafWidth - PULL_INSET_MM;
					var pullY = template.getType() == CabinetType.WALL ? rowY + PULL_INSET_MM
							: rowY + row.heightMm() - PULL_INSET_MM - PULL_MM;
					shapes.add(line(pullX, pullY, pullX, pullY + Math.min(PULL_MM, row.heightMm() / 3), Stroke.OUTLINE));
				}
			} else {
				var fill = row.kind() == FrontKind.OPENING ? Fill.DARK : Fill.LIGHT;
				shapes.add(rect(frontLeft + gap / 2, rowY, frontWidth - gap, row.heightMm(), Stroke.THIN, fill));
				if (row.kind() == FrontKind.DRAWER) {
					var middle = frontLeft + frontWidth / 2;
					var pullY = rowY + row.heightMm() - Math.min(PULL_INSET_MM, row.heightMm() / 3);
					shapes.add(line(middle - PULL_MM / 2, pullY, middle + PULL_MM / 2, pullY, Stroke.OUTLINE));
				}
			}
		}

		if (blind > 0) {
			var blindX = blindLeft ? x : x + width - blind;
			shapes.add(rect(blindX + gap / 2, boxY + gap / 2, blind - gap, boxHeight - gap, Stroke.THIN, Fill.MEDIUM));
		}

		if (interior) {
			var dividers = cabinet.dividers();
			var inside = boxHeight - 2 * PANEL_MM;
			dividers.forEach(position -> shapes.add(line(x + position, boxY + PANEL_MM, x + position,
					boxY + PANEL_MM + inside, Stroke.HIDDEN)));
			var bounds = new java.util.ArrayList<Double>();
			bounds.add(PANEL_MM);
			bounds.addAll(dividers);
			bounds.add(width - PANEL_MM);
			for (int section = 0; section < bounds.size() - 1; section++) {
				for (int shelf = 1; shelf <= cabinet.getShelfCount(); shelf++) {
					var shelfY = boxY + PANEL_MM + inside * shelf / (cabinet.getShelfCount() + 1);
					shapes.add(line(x + bounds.get(section) + PANEL_MM / 2, shelfY, x + bounds.get(section + 1) - PANEL_MM / 2,
							shelfY, Stroke.HIDDEN));
				}
			}
		}

		if (cabinet.isFinishedLeft()) {
			shapes.add(rect(x - FINISHED_END_MM, y, FINISHED_END_MM, cabinet.getHeightMm(), Stroke.THIN, Fill.MEDIUM));
		}
		if (cabinet.isFinishedRight()) {
			shapes.add(rect(x + width, y, FINISHED_END_MM, cabinet.getHeightMm(), Stroke.THIN, Fill.MEDIUM));
		}
		if (cabinet.isCrown() && template.getType() != CabinetType.BASE) {
			shapes.add(rect(x - FINISHED_END_MM, y + cabinet.getHeightMm(), width + 2 * FINISHED_END_MM, CROWN_HEIGHT_MM,
					Stroke.THIN, Fill.MEDIUM));
		}
	}
}
