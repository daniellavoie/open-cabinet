package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.PartsTable;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Line;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Sheet;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.Assembly.AssembledPart;
import dev.daniellavoie.opencabinet.project.domain.Assembly;
import dev.daniellavoie.opencabinet.project.domain.CabinetAssemblies;
import dev.daniellavoie.opencabinet.project.domain.FrontLayout;
import dev.daniellavoie.opencabinet.project.domain.RoomLayout;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.Side;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.Axis;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.PartKind;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Lays out the Shop Drawings of a design, in millimetres: a plan view, an Elevation per Wall and a
 * Sheet per Cabinet with its front and side and its Parts. Fronts are sized like the Parts
 * ({@link FrontLayout}, the Shop's door gap), so the drawings match the Cut List.
 */
final class ShopDrawingsGenerator {
	/**
	 * Same as the 3D view.
	 */
	static final double WALL_THICKNESS_MM = 100;
	static final double FRONT_THICKNESS_MM = 19;
	static final double TOE_KICK_SETBACK_MM = 75;
	static final double LABEL_CLEARANCE_MM = 90;
	/**
	 * How far behind the nearest Part a Part still counts as seen in a front or side view.
	 */
	static final double SEEN_DEPTH_MM = 30;

	/**
	 * What the drawings are made from. {@code labels} map Cabinet ids to C1, C2… in reading order;
	 * {@code parts} are every Part of the Project, Trim included.
	 */
	record Source(Design design, Map<String, CabinetTemplate> templates, Map<String, String> labels,
			List<Part> parts, Map<UUID, Material> materials, ConstructionStandard standard, Units units,
			DrawingLanguage language) {
		String length(double mm) {
			return LengthFormat.format(mm, units);
		}

		CabinetTemplate template(Cabinet cabinet) {
			return templates.get(cabinet.templateKey());
		}
	}

	private ShopDrawingsGenerator() {
	}

	static List<Sheet> generate(Source source) {
		var sheets = new ArrayList<Sheet>();
		sheets.add(plan(source));
		var walls = source.design().getWalls();
		IntStream.range(0, walls.size())
				.forEach(index -> sheets.add(elevation(source, walls.get(index), index + 1, true)));
		cabinetsInOrder(source).forEach(cabinet -> sheets.add(cabinet(source, cabinet)));
		return sheets;
	}

	/**
	 * The Room from above, as in the 3D view: the first Wall runs left to right and Cabinets stand
	 * on the side facing the reader. Wall Cabinets are dashed.
	 */
	static Sheet plan(Source source) {
		var language = source.language();
		var design = source.design();
		var placements = RoomLayout.layout(design.getWalls());
		var shapes = new ArrayList<Shape>();

		var walls = design.getWalls();
		for (int index = 0; index < walls.size(); index++) {
			var wall = walls.get(index);
			var placement = placements.get(wall.getId());
			// A Wall that turns into the next one runs on through the corner, so the corner is filled.
			var turns = index < walls.size() - 1 && Math.abs(wall.getAngleToNextDegrees()) > 0;
			shapes.add(new Outline(plan(placement.rectangle(0, wall.getLengthMm() + (turns ? WALL_THICKNESS_MM : 0),
					-WALL_THICKNESS_MM, 0)), Stroke.WALL));
			shapes.add(new Dimension(plan(placement.at(0, -WALL_THICKNESS_MM)),
					plan(placement.at(wall.getLengthMm(), -WALL_THICKNESS_MM)), 1,
					language.wall(index + 1) + " : " + source.length(wall.getLengthMm())));
		}

		for (var opening : list(design.getOpenings())) {
			Optional.ofNullable(placements.get(opening.getWallId())).ifPresent(placement -> shapes.add(new Outline(
					plan(placement.rectangle(opening.getOffsetMm(), opening.getOffsetMm() + opening.getWidthMm(),
							-WALL_THICKNESS_MM, 0)),
					Stroke.OPENING)));
		}

		for (var appliance : list(design.getAppliances())) {
			Optional.ofNullable(placements.get(appliance.getWallId())).ifPresent(placement -> {
				var end = appliance.getOffsetMm() + appliance.getWidthMm();
				shapes.add(new Outline(plan(placement.rectangle(appliance.getOffsetMm(), end, 0,
						appliance.getDepthMm())), Stroke.HIDDEN));
				shapes.add(new Label(plan(placement.at((appliance.getOffsetMm() + end) / 2, appliance.getDepthMm() / 2)),
						language.appliance(appliance.getKind())));
			});
		}

		for (var filler : list(design.getFillers())) {
			Optional.ofNullable(placements.get(filler.getWallId())).ifPresent(placement -> shapes.add(new Outline(
					plan(placement.rectangle(filler.getOffsetMm(), filler.getOffsetMm() + filler.getWidthMm(), 0,
							filler.getDepthMm())),
					Stroke.OUTLINE)));
		}

		for (var cabinet : cabinetsInOrder(source)) {
			var placement = placements.get(cabinet.getWallId());
			var floor = source.template(cabinet).getType().standsOnFloor();
			var end = cabinet.getOffsetMm() + cabinet.getWidthMm();
			shapes.add(new Outline(plan(placement.rectangle(cabinet.getOffsetMm(), end, 0, cabinet.getDepthMm())),
					floor ? Stroke.OUTLINE : Stroke.HIDDEN));
			// Wall Cabinets' labels sit nearer the Wall, so they clear the base Cabinets' below.
			shapes.add(new Label(plan(placement.at((cabinet.getOffsetMm() + end) / 2,
					cabinet.getDepthMm() * (floor ? 0.65 : 0.3))), source.labels().get(cabinet.getId())));
		}

		return Sheet.builder()
				.title(language.pick("Vue en plan", "Plan view"))
				.views(List.of(View.builder().title("").shapes(shapes).build()))
				.notes(List.of(language.pick("Caissons du haut en pointillé, vus du dessus.",
						"Wall cabinets dashed, seen from above.")))
				.build();
	}

	/**
	 * One Wall seen from inside the Room, its start on the left: Openings, Appliances (dashed),
	 * Fillers and Cabinets with their Fronts. Widths are chained under the floor-standing row and over
	 * the wall Cabinets; the Wall's height on the left, wall Cabinets' heights from the floor on the
	 * right. For the Client ({@code forShop} false), only the Wall's length and height are dimensioned
	 * and Cabinets are not labelled.
	 */
	static Sheet elevation(Source source, Wall wall, int number, boolean forShop) {
		var language = source.language();
		var design = source.design();
		var length = wall.getLengthMm();
		var shapes = new ArrayList<Shape>();
		shapes.add(Outline.box(0, 0, length, wall.getHeightMm(), Stroke.THIN));

		var floorItems = new ArrayList<double[]>();
		for (var opening : onWall(design.getOpenings(), wall, item -> item.getWallId())) {
			shapes.add(Outline.box(opening.getOffsetMm(), opening.getElevationMm(), opening.getWidthMm(),
					opening.getHeightMm(), Stroke.THIN));
			shapes.add(new Label(new Point(opening.getOffsetMm() + opening.getWidthMm() / 2,
					opening.getElevationMm() + opening.getHeightMm() / 2), language.opening(opening.getKind())));
		}
		for (var appliance : onWall(design.getAppliances(), wall, item -> item.getWallId())) {
			shapes.add(Outline.box(appliance.getOffsetMm(), appliance.getElevationMm(), appliance.getWidthMm(),
					appliance.getHeightMm(), Stroke.HIDDEN));
			shapes.add(new Label(new Point(appliance.getOffsetMm() + appliance.getWidthMm() / 2,
					appliance.getElevationMm() + appliance.getHeightMm() / 2), language.appliance(appliance.getKind())));
			if (appliance.getElevationMm() < 1) {
				floorItems.add(new double[] { appliance.getOffsetMm(), appliance.getWidthMm() });
			}
		}
		for (var filler : onWall(design.getFillers(), wall, item -> item.getWallId())) {
			shapes.add(Outline.box(filler.getOffsetMm(), filler.getElevationMm(), filler.getWidthMm(),
					filler.getHeightMm(), Stroke.OUTLINE));
			if (filler.getElevationMm() < 1) {
				floorItems.add(new double[] { filler.getOffsetMm(), filler.getWidthMm() });
			}
		}

		var cabinets = cabinetsInOrder(source).stream().filter(cabinet -> cabinet.getWallId().equals(wall.getId()))
				.toList();
		var notes = new ArrayList<String>();
		var upper = new ArrayList<Cabinet>();
		for (var cabinet : cabinets) {
			var template = source.template(cabinet);
			shapes.addAll(front(source, cabinet, template, cabinet.getOffsetMm(), cabinet.getElevationMm()));
			if (!forShop) {
				continue;
			}
			shapes.add(new Label(new Point(cabinet.getOffsetMm() + cabinet.getWidthMm() / 2, labelHeight(cabinet,
					template)), source.labels().get(cabinet.getId())));
			if (cabinet.getElevationMm() < 1) {
				floorItems.add(new double[] { cabinet.getOffsetMm(), cabinet.getWidthMm() });
			} else {
				upper.add(cabinet);
			}
			notes.add(source.labels().get(cabinet.getId()) + "  " + language.templateName(cabinet.getTemplateCode())
					+ "  " + size(source, cabinet));
		}

		if (!forShop) {
			shapes.add(new Dimension(new Point(length, 0), new Point(0, 0), 1, source.length(length)));
			shapes.add(new Dimension(new Point(0, 0), new Point(0, wall.getHeightMm()), 1,
					source.length(wall.getHeightMm())));
			return Sheet.builder()
					.title(language.pick("Élévation — ", "Elevation — ") + language.wall(number))
					.views(List.of(View.builder().title("").shapes(shapes).build()))
					.notes(List.of())
					.build();
		}

		floorItems.sort(Comparator.comparingDouble(item -> item[0]));
		for (var item : floorItems) {
			shapes.add(new Dimension(new Point(item[0] + item[1], 0), new Point(item[0], 0), 1,
					source.length(item[1])));
		}
		shapes.add(new Dimension(new Point(length, 0), new Point(0, 0), 2, source.length(length)));
		shapes.add(new Dimension(new Point(0, 0), new Point(0, wall.getHeightMm()), 1,
				source.length(wall.getHeightMm())));

		var upperTop = upper.stream().mapToDouble(cabinet -> cabinet.getElevationMm() + cabinet.getHeightMm()).max()
				.orElse(0);
		for (var cabinet : upper) {
			shapes.add(new Dimension(new Point(cabinet.getOffsetMm(), upperTop),
					new Point(cabinet.getOffsetMm() + cabinet.getWidthMm(), upperTop), 1,
					source.length(cabinet.getWidthMm())));
		}
		var heights = upper.stream().map(Cabinet::getElevationMm).distinct().sorted().toList();
		for (int index = 0; index < heights.size(); index++) {
			shapes.add(new Dimension(new Point(length, heights.get(index)), new Point(length, 0), index + 1,
					source.length(heights.get(index))));
		}

		return Sheet.builder()
				.title(language.pick("Élévation — ", "Elevation — ") + language.wall(number))
				.views(List.of(View.builder().title("").shapes(shapes).build()))
				.notes(notes)
				.build();
	}

	/**
	 * One Cabinet: its front with the height of each front row, its side, what it is built with and
	 * its Parts.
	 */
	static Sheet cabinet(Source source, Cabinet cabinet) {
		var language = source.language();
		var template = source.template(cabinet);
		var label = source.labels().get(cabinet.getId());
		var width = cabinet.getWidthMm();
		var height = cabinet.getHeightMm();
		var boxBottom = boxBottom(source, template);

		var front = new ArrayList<Shape>(front(source, cabinet, template, 0, 0));
		for (var divider : cabinet.dividers()) {
			front.add(new Line(new Point(divider, boxBottom), new Point(divider, height), Stroke.HIDDEN));
		}
		front.add(new Dimension(new Point(width, 0), new Point(0, 0), 1, source.length(width)));
		front.add(new Dimension(new Point(0, 0), new Point(0, height), 1, source.length(height)));
		for (var row : frontHeights(source, cabinet, template)) {
			front.add(new Dimension(new Point(width, row[1]), new Point(width, row[0]), 1, source.length(row[1] - row[0])));
		}
		if (boxBottom > 0) {
			front.add(new Dimension(new Point(width, boxBottom), new Point(width, 0), 1, source.length(boxBottom)));
		}

		var depth = cabinet.getDepthMm();
		var side = new ArrayList<Shape>();
		if (template.getAssembly() != null) {
			side.addAll(seen(assembled(source, cabinet, template), Axis.X, 0, 0));
		} else {
			side.add(Outline.box(0, boxBottom, depth - FRONT_THICKNESS_MM, height - boxBottom, Stroke.OUTLINE));
			side.add(Outline.box(depth - FRONT_THICKNESS_MM, boxBottom, FRONT_THICKNESS_MM, height - boxBottom,
					Stroke.THIN));
			if (boxBottom > 0) {
				side.add(Outline.box(0, 0, depth - FRONT_THICKNESS_MM - TOE_KICK_SETBACK_MM, boxBottom, Stroke.THIN));
			}
		}
		side.add(new Dimension(new Point(depth, 0), new Point(0, 0), 1, source.length(depth)));

		var notes = new ArrayList<String>();
		notes.add(language.templateName(cabinet.getTemplateCode()) + ", version " + cabinet.getTemplateVersion() + "  ·  " + wallName(source, cabinet) + "  ·  " + size(source, cabinet));
		var interior = new ArrayList<String>();
		if (template.getMaxShelfCount() > 0) {
			interior.add(language.pick("Tablettes par section : ", "Shelves per section: ") + cabinet.getShelfCount());
		}
		if (!cabinet.dividers().isEmpty()) {
			interior.add(language.pick("Séparateurs : ", "Dividers: ") + cabinet.dividers().size());
		}
		interior.add(cabinet.frontsOrdered() ? language.pick("Façades commandées", "Fronts ordered")
				: language.pick("Façades fabriquées à l'atelier", "Fronts made in the Shop"));
		if (cabinet.isFinishedLeft() || cabinet.isFinishedRight()) {
			interior.add(language.pick("Panneau de finition : ", "Finished end: ") + Stream.of(
					cabinet.isFinishedLeft() ? language.pick("gauche", "left") : null,
					cabinet.isFinishedRight() ? language.pick("droite", "right") : null)
					.filter(Objects::nonNull).collect(Collectors.joining(", ")));
		}
		if (cabinet.isCrown() && template.getType() != CabinetType.BASE) {
			interior.add(language.pick("Moulure couronne", "Crown moulding"));
		}
		notes.add(String.join("  ·  ", interior));

		return Sheet.builder()
				.title(label + " — " + language.templateName(cabinet.getTemplateCode()))
				.views(List.of(
						View.builder().title(language.pick("Face", "Front")).shapes(front).build(),
						View.builder().title(language.pick("Côté droit", "Right side")).shapes(side).build()))
				.notes(notes)
				.parts(parts(source, cabinet, label))
				.build();
	}

	/**
	 * A Cabinet's box, toe kick line and Fronts, from its bottom-left corner at {@code (x, y)}.
	 * Blind corners leave {@code blindWidthMm} on their blind side, covered by a blind panel.
	 */
	static List<Shape> front(Source source, Cabinet cabinet, CabinetTemplate template, double x, double y) {
		if (template.getAssembly() != null) {
			var shapes = new ArrayList<Shape>();
			shapes.add(Outline.box(x, y, cabinet.getWidthMm(), cabinet.getHeightMm(), Stroke.OUTLINE));
			shapes.addAll(seen(assembled(source, cabinet, template), Axis.Z, x, y));
			return shapes;
		}
		var shapes = new ArrayList<Shape>();
		var width = cabinet.getWidthMm();
		var boxBottom = boxBottom(source, template);
		var boxHeight = cabinet.getHeightMm() - boxBottom;
		var gap = source.standard().getDoorGapMm();

		shapes.add(Outline.box(x, y, width, cabinet.getHeightMm(), Stroke.OUTLINE));
		if (boxBottom > 0) {
			shapes.add(new Line(new Point(x, y + boxBottom), new Point(x + width, y + boxBottom), Stroke.THIN));
		}

		var blind = template.getBlindWidthMm();
		var blindOnLeft = blind > 0 && cabinet.effectiveBlindSide() == Side.LEFT;
		var frontWidth = FrontLayout.frontWidth(template, width);
		var frontsStart = x + (blindOnLeft ? blind : 0);
		for (var row : FrontLayout.rows(template, boxHeight, gap)) {
			var bottom = y + boxBottom + row.bottomMm();
			if (row.kind() == FrontKind.DOOR) {
				var leaves = FrontLayout.doorLeaves(frontWidth);
				var leafWidth = (frontWidth - gap * leaves) / leaves;
				for (int index = 0; index < leaves; index++) {
					shapes.add(Outline.box(frontsStart + gap / 2 + index * (leafWidth + gap), bottom, leafWidth,
							row.heightMm(), Stroke.THIN));
				}
			} else {
				shapes.add(Outline.box(frontsStart + gap / 2, bottom, frontWidth - gap, row.heightMm(),
						row.kind() == FrontKind.OPENING ? Stroke.HIDDEN : Stroke.THIN));
			}
		}
		if (blind > 0) {
			var panelX = blindOnLeft ? x : x + width - blind;
			shapes.add(Outline.box(panelX + gap / 2, y + boxBottom + gap / 2, blind - gap, boxHeight - gap,
					Stroke.THIN));
		}
		return shapes;
	}

	/**
	 * An assembled Cabinet's Parts at its size (ADR 0006).
	 */
	static List<AssembledPart> assembled(Source source, Cabinet cabinet, CabinetTemplate template) {
		return Assembly.assemble(template.getAssembly(), CabinetAssemblies.context(cabinet, source.standard(),
				materialId -> Optional.ofNullable(source.materials().get(materialId)).map(Material::getThicknessMm)
						.orElse(0d)))
				.parts();
	}

	/**
	 * The Parts seen when looking along {@code axis} from its high end (the front for z, the right
	 * side for x): those within a front's thickness of the nearest, Fronts thin, others outlined. In
	 * a side view, depth runs left to right from the back.
	 */
	static List<Shape> seen(List<AssembledPart> parts, Axis axis, double x, double y) {
		var nearest = parts.stream().mapToDouble(part -> part.box().max(axis)).max().orElse(0);
		var horizontal = axis == Axis.Z ? Axis.X : Axis.Z;
		return parts.stream()
				.filter(part -> part.kind() == PartKind.PANEL && part.box().max(axis) >= nearest - SEEN_DEPTH_MM)
				.<Shape>map(part -> Outline.box(x + part.box().min(horizontal), y + part.box().y0(),
						part.box().size(horizontal), part.box().size(Axis.Y), part.front() ? Stroke.THIN : Stroke.OUTLINE))
				.toList();
	}

	/**
	 * Each Front's bottom and top, for dimensioning: an assembled template's front Parts, or a starter
	 * template's front rows.
	 */
	private static List<double[]> frontHeights(Source source, Cabinet cabinet, CabinetTemplate template) {
		if (template.getAssembly() != null) {
			// Door leaves side by side share a row: one dimension each.
			var rows = new LinkedHashMap<String, double[]>();
			assembled(source, cabinet, template).stream()
					.filter(AssembledPart::front)
					.forEach(part -> rows.putIfAbsent(Math.round(part.box().y0() * 10) + "/" + Math.round(part.box().y1() * 10),
							new double[] { part.box().y0(), part.box().y1() }));
			return List.copyOf(rows.values());
		}
		var boxBottom = boxBottom(source, template);
		return FrontLayout.rows(template, cabinet.getHeightMm() - boxBottom, source.standard().getDoorGapMm()).stream()
				.map(row -> new double[] { boxBottom + row.bottomMm(), boxBottom + row.bottomMm() + row.heightMm() })
				.toList();
	}

	/**
	 * The Cabinet's own Parts, and Trim cut for it, identical ones counted together.
	 */
	static PartsTable parts(Source source, Cabinet cabinet, String label) {
		var language = source.language();
		var rows = new LinkedHashMap<List<String>, Integer>();
		source.parts().stream()
				.filter(part -> cabinet.getId().equals(part.getCabinetId())
						|| part.getCabinetId() == null && label.equals(part.getCabinetLabel()))
				.forEach(part -> rows.merge(List.of(
						language.part(part.getRole(), part.getName()),
						Optional.ofNullable(source.materials().get(part.getMaterialId())).map(Material::getName).orElse("")
								+ (part.isOrdered() ? language.pick(" (commandée)", " (ordered)") : ""),
						source.length(part.getLengthMm()),
						source.length(part.getWidthMm()),
						source.length(part.getThicknessMm()),
						part.getBandedLengthEdges() + " / " + part.getBandedWidthEdges()), 1, Integer::sum));

		return PartsTable.builder()
				.headers(List.of(language.pick("Pièce", "Part"), language.pick("Matériau", "Material"),
						language.pick("Longueur", "Length"), language.pick("Largeur", "Width"),
						language.pick("Épaisseur", "Thickness"), language.pick("Chants (long. / larg.)",
								"Banded edges (len. / wid.)"),
						language.pick("Qté", "Qty")))
				.rows(rows.entrySet().stream()
						.map(entry -> Stream.concat(entry.getKey().stream(), Stream.of(String.valueOf(entry.getValue())))
								.toList())
						.toList())
				.build();
	}

	/**
	 * Clear of the Fronts' lines: over base Cabinets, under wall Cabinets, halfway up tall ones.
	 */
	private static double labelHeight(Cabinet cabinet, CabinetTemplate template) {
		return switch (template.getType()) {
		case BASE -> cabinet.getElevationMm() + cabinet.getHeightMm() + LABEL_CLEARANCE_MM;
		case WALL -> cabinet.getElevationMm() - LABEL_CLEARANCE_MM;
		case TALL -> cabinet.getElevationMm() + cabinet.getHeightMm() / 2;
		};
	}

	private static double boxBottom(Source source, CabinetTemplate template) {
		return template.getType().standsOnFloor() ? source.standard().getToeKickHeightMm() : 0;
	}

	private static String size(Source source, Cabinet cabinet) {
		return source.length(cabinet.getWidthMm()) + " × " + source.length(cabinet.getHeightMm()) + " × "
				+ source.length(cabinet.getDepthMm());
	}

	private static String wallName(Source source, Cabinet cabinet) {
		var walls = source.design().getWalls();
		return IntStream.range(0, walls.size())
				.filter(index -> walls.get(index).getId().equals(cabinet.getWallId()))
				.mapToObj(index -> source.language().wall(index + 1))
				.findFirst()
				.orElse("");
	}

	/**
	 * Cabinets in label order (C1, C2…), skipping any whose Wall or Template Version is unknown.
	 */
	static List<Cabinet> cabinetsInOrder(Source source) {
		var wallIds = source.design().getWalls().stream().map(Wall::getId).toList();
		var order = new ArrayList<>(source.labels().keySet());
		return list(source.design().getCabinets()).stream()
				.filter(cabinet -> wallIds.contains(cabinet.getWallId()) && source.template(cabinet) != null)
				.sorted(Comparator.comparingInt(cabinet -> order.indexOf(cabinet.getId())))
				.toList();
	}

	private static <T> List<T> onWall(List<T> items, Wall wall, Function<T, String> wallId) {
		return list(items).stream().filter(item -> wall.getId().equals(wallId.apply(item))).toList();
	}

	private static <T> List<T> list(List<T> items) {
		return Optional.ofNullable(items).orElse(List.of());
	}

	/**
	 * Plan coordinates to the drawing's: seen from above, as the 3D view's top view.
	 */
	private static Point plan(RoomLayout.Point point) {
		return new Point(point.x(), -point.z());
	}

	private static List<Point> plan(List<RoomLayout.Point> points) {
		return points.stream().map(ShopDrawingsGenerator::plan).toList();
	}
}
