package dev.daniellavoie.opencabinet.drawing.domain;

import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.horizontal;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.line;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.rect;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.text;
import static dev.daniellavoie.opencabinet.drawing.domain.Shapes.vertical;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;
import dev.daniellavoie.opencabinet.project.domain.RoomLayout;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;

/**
 * The Shop Drawings of a Design: a plan, an Elevation per Wall and a detail per Cabinet, all to
 * scale and dimensioned in the Shop's units.
 */
public final class ShopDrawings {
	static final double WALL_THICKNESS_MM = 100;
	static final double DIMENSION_STEP_MM = 180;
	static final double DETAIL_GAP_MM = 500;
	static final double FRONT_THICKNESS_MM = 19;
	static final double TOE_KICK_SETBACK_MM = 75;
	static final double SHELF_SETBACK_MM = 25;

	private ShopDrawings() {
	}

	public static List<Drawing> all(DrawingContext context) {
		var drawings = new ArrayList<Drawing>();
		drawings.add(plan(context));
		IntStream.range(0, context.walls().size()).forEach(index -> drawings.add(elevation(context, index)));
		context.cabinets().stream()
				.filter(cabinet -> context.templates().containsKey(cabinet.getTemplateCode()))
				.forEach(cabinet -> drawings.add(cabinet(context, cabinet)));
		return drawings;
	}

	/**
	 * The Room from above: Walls with their Openings, Cabinets on the floor solid, Cabinets hung
	 * above dashed, Appliances and Fillers, and each Wall's length outside it.
	 */
	public static Drawing plan(DrawingContext context) {
		var shapes = new ArrayList<Shape>();
		var placements = RoomLayout.layout(context.walls());

		for (int index = 0; index < context.walls().size(); index++) {
			var wall = context.walls().get(index);
			var placement = placements.get(wall.getId());
			shapes.add(polygon(placement.rectangle(0, wall.getLengthMm(), -WALL_THICKNESS_MM, 0), Stroke.OUTLINE,
					Fill.WALL));
			var outside = DIMENSION_STEP_MM + WALL_THICKNESS_MM;
			shapes.add(new Shape.Dimension(plan(placement.at(0, 0)), plan(placement.at(wall.getLengthMm(), 0)), outside,
					context.wallTitle(index) + " — " + context.length(wall.getLengthMm())));
		}

		for (var opening : context.openings()) {
			var placement = placements.get(opening.getWallId());
			if (placement == null) {
				continue;
			}
			var end = opening.getOffsetMm() + opening.getWidthMm();
			shapes.add(polygon(placement.rectangle(opening.getOffsetMm(), end, -WALL_THICKNESS_MM, 0), Stroke.THIN,
					opening.getKind() == OpeningKind.WINDOW ? Fill.WINDOW : Fill.WHITE));
			if (opening.getKind() == OpeningKind.WINDOW) {
				shapes.add(new Shape.Line(plan(placement.at(opening.getOffsetMm(), -WALL_THICKNESS_MM / 2)),
						plan(placement.at(end, -WALL_THICKNESS_MM / 2)), Stroke.THIN));
			}
		}

		for (var appliance : context.appliances()) {
			var placement = placements.get(appliance.getWallId());
			if (placement == null) {
				continue;
			}
			var footprint = placement.rectangle(appliance.getOffsetMm(), appliance.getOffsetMm() + appliance.getWidthMm(),
					0, appliance.getDepthMm());
			shapes.add(polygon(footprint, appliance.getElevationMm() > 0 ? Stroke.HIDDEN : Stroke.THIN, Fill.APPLIANCE));
			shapes.add(new Shape.Text(centre(footprint), context.applianceName(appliance.getKind()), TextStyle.SMALL, 0));
		}

		for (var filler : context.fillers()) {
			var placement = placements.get(filler.getWallId());
			if (placement != null) {
				shapes.add(polygon(placement.rectangle(filler.getOffsetMm(), filler.getOffsetMm() + filler.getWidthMm(),
						filler.getDepthMm() - FRONT_THICKNESS_MM, filler.getDepthMm()), Stroke.THIN, Fill.MEDIUM));
			}
		}

		// Floor Cabinets first, so Cabinets hung above them are drawn dashed on top.
		var cabinets = context.cabinets().stream()
				.sorted((first, second) -> Boolean.compare(hung(context, first), hung(context, second)))
				.toList();
		for (var cabinet : cabinets) {
			var placement = placements.get(cabinet.getWallId());
			if (placement == null) {
				continue;
			}
			var footprint = placement.rectangle(cabinet.getOffsetMm(), cabinet.getOffsetMm() + cabinet.getWidthMm(), 0,
					cabinet.getDepthMm());
			var hung = hung(context, cabinet);
			shapes.add(polygon(footprint, hung ? Stroke.HIDDEN : Stroke.OUTLINE, hung ? Fill.NONE : Fill.LIGHT));
			var labelAt = centre(hung ? placement.rectangle(cabinet.getOffsetMm(),
					cabinet.getOffsetMm() + cabinet.getWidthMm(), 0, cabinet.getDepthMm() / 2) : footprint);
			shapes.add(new Shape.Text(labelAt, context.label(cabinet), TextStyle.LABEL, 0));
		}

		return new Drawing(context.language().pick("Plan", "Plan"), shapes);
	}

	/**
	 * One Wall seen straight on: its Openings, Appliances, Fillers and Cabinets with their Fronts,
	 * Cabinet widths along the floor and above the Cabinets hung on it, the Wall's length below and
	 * the heights of each kind of Cabinet on the left.
	 */
	public static Drawing elevation(DrawingContext context, int wallIndex) {
		var wall = context.walls().get(wallIndex);
		var length = wall.getLengthMm();
		var height = wall.getHeightMm();
		var shapes = new ArrayList<Shape>();

		shapes.add(rect(0, 0, length, height, Stroke.THIN, Fill.NONE));

		for (var opening : context.openings()) {
			if (opening.getWallId().equals(wall.getId())) {
				shapes.add(rect(opening.getOffsetMm(), opening.getElevationMm(), opening.getWidthMm(),
						opening.getHeightMm(), Stroke.THIN, opening.getKind() == OpeningKind.WINDOW ? Fill.WINDOW : Fill.WHITE));
			}
		}
		for (var appliance : context.appliances()) {
			if (appliance.getWallId().equals(wall.getId())) {
				shapes.add(rect(appliance.getOffsetMm(), appliance.getElevationMm(), appliance.getWidthMm(),
						appliance.getHeightMm(), Stroke.THIN, Fill.APPLIANCE));
				shapes.add(text(appliance.getOffsetMm() + appliance.getWidthMm() / 2,
						appliance.getElevationMm() + appliance.getHeightMm() / 2, context.applianceName(appliance.getKind()),
						TextStyle.SMALL));
			}
		}
		for (var filler : context.fillers()) {
			if (filler.getWallId().equals(wall.getId())) {
				shapes.add(rect(filler.getOffsetMm(), filler.getElevationMm(), filler.getWidthMm(), filler.getHeightMm(),
						Stroke.THIN, Fill.LIGHT));
			}
		}

		var onWall = context.cabinets().stream().filter(cabinet -> cabinet.getWallId().equals(wall.getId())).toList();
		var heights = new LinkedHashSet<List<Double>>();
		for (var cabinet : onWall) {
			var template = context.templates().get(cabinet.getTemplateCode());
			if (template == null) {
				continue;
			}
			CabinetFront.draw(shapes, cabinet, template, cabinet.getOffsetMm(), cabinet.getElevationMm(), context, false);
			shapes.add(text(cabinet.getOffsetMm() + cabinet.getWidthMm() / 2,
					cabinet.getElevationMm() + cabinet.getHeightMm() + (hung(context, cabinet) ? -60 : 60),
					context.label(cabinet), TextStyle.LABEL));

			var left = cabinet.getOffsetMm();
			var right = left + cabinet.getWidthMm();
			if (hung(context, cabinet)) {
				shapes.add(horizontal(left, right, cabinet.getElevationMm() + cabinet.getHeightMm(), DIMENSION_STEP_MM,
						context.length(cabinet.getWidthMm())));
			} else {
				shapes.add(horizontal(left, right, 0, -DIMENSION_STEP_MM, context.length(cabinet.getWidthMm())));
			}
			heights.add(List.of(cabinet.getElevationMm(), cabinet.getElevationMm() + cabinet.getHeightMm()));
		}

		shapes.add(line(-WALL_THICKNESS_MM, 0, length + WALL_THICKNESS_MM, 0, Stroke.WALL));
		shapes.add(horizontal(0, length, 0, -2 * DIMENSION_STEP_MM, context.length(length)));
		shapes.add(vertical(length, 0, height, -DIMENSION_STEP_MM, context.length(height)));
		var tier = 1;
		for (var span : heights) {
			var bottom = span.get(0);
			var top = span.get(1);
			if (bottom > 0.5) {
				shapes.add(vertical(0, 0, bottom, tier * DIMENSION_STEP_MM, context.length(bottom)));
			}
			shapes.add(vertical(0, bottom, top, tier * DIMENSION_STEP_MM, context.length(top - bottom)));
			tier++;
		}

		return new Drawing(context.language().pick("Élévation — ", "Elevation — ") + context.wallTitle(wallIndex),
				shapes);
	}

	/**
	 * One Cabinet: its front with dividers and shelves dashed behind the doors, and its side with the
	 * toe kick, shelves and Front thickness, both dimensioned.
	 */
	public static Drawing cabinet(DrawingContext context, Cabinet cabinet) {
		var template = context.templates().get(cabinet.getTemplateCode());
		var width = cabinet.getWidthMm();
		var height = cabinet.getHeightMm();
		var depth = cabinet.getDepthMm();
		var shapes = new ArrayList<Shape>();

		CabinetFront.draw(shapes, cabinet, template, 0, 0, context, true);
		shapes.add(horizontal(0, width, 0, -DIMENSION_STEP_MM, context.length(width)));
		shapes.add(vertical(0, 0, height, DIMENSION_STEP_MM, context.length(height)));
		shapes.add(text(width / 2, -2 * DIMENSION_STEP_MM, context.language().pick("Face", "Front"), TextStyle.SMALL));

		var floor = template.getType().standsOnFloor();
		var boxBottom = floor ? context.toeKickHeightMm() : 0;
		var boxHeight = height - boxBottom;
		var boxDepth = depth - FRONT_THICKNESS_MM;
		var side = width + DETAIL_GAP_MM;

		shapes.add(rect(side, boxBottom, boxDepth, boxHeight, Stroke.OUTLINE, Fill.WHITE));
		shapes.add(rect(side + boxDepth, boxBottom, FRONT_THICKNESS_MM, boxHeight, Stroke.THIN, Fill.LIGHT));
		if (floor && boxBottom > 0) {
			shapes.add(rect(side + boxDepth - TOE_KICK_SETBACK_MM - CabinetFront.PANEL_MM, 0, CabinetFront.PANEL_MM,
					boxBottom, Stroke.THIN, Fill.MEDIUM));
		}
		var inside = boxHeight - 2 * CabinetFront.PANEL_MM;
		for (int shelf = 1; shelf <= cabinet.getShelfCount(); shelf++) {
			var shelfY = boxBottom + CabinetFront.PANEL_MM + inside * shelf / (cabinet.getShelfCount() + 1);
			shapes.add(rect(side, shelfY - CabinetFront.PANEL_MM / 2, boxDepth - SHELF_SETBACK_MM, CabinetFront.PANEL_MM,
					Stroke.THIN, Fill.LIGHT));
		}
		shapes.add(horizontal(side, side + depth, 0, -DIMENSION_STEP_MM, context.length(depth)));
		shapes.add(vertical(side + depth, boxBottom, height, -DIMENSION_STEP_MM, context.length(boxHeight)));
		if (floor && boxBottom > 0) {
			shapes.add(vertical(side + depth, 0, boxBottom, -DIMENSION_STEP_MM, context.length(boxBottom)));
		}
		shapes.add(text(side + depth / 2, -2 * DIMENSION_STEP_MM, context.language().pick("Côté", "Side"),
				TextStyle.SMALL));

		return new Drawing(context.label(cabinet) + " — " + context.templateName(cabinet.getTemplateCode()), shapes);
	}

	/**
	 * Hung on the Wall rather than standing on the floor.
	 */
	static boolean hung(DrawingContext context, Cabinet cabinet) {
		var template = context.templates().get(cabinet.getTemplateCode());
		return template != null && template.getType() == CabinetType.WALL;
	}

	/**
	 * Plan coordinates as drawing coordinates: seen from above, the Room's z runs down the page.
	 */
	static Point plan(RoomLayout.Point point) {
		return new Point(point.x(), -point.z());
	}

	private static Shape.Polygon polygon(List<RoomLayout.Point> points, Stroke stroke, Fill fill) {
		return new Shape.Polygon(points.stream().map(ShopDrawings::plan).toList(), stroke, fill);
	}

	private static Point centre(List<RoomLayout.Point> points) {
		return plan(new RoomLayout.Point(points.stream().mapToDouble(RoomLayout.Point::x).average().orElse(0),
				points.stream().mapToDouble(RoomLayout.Point::z).average().orElse(0)));
	}
}
