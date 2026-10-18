package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.daniellavoie.opencabinet.drawing.domain.ShopDrawingsGenerator.Source;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Line;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.RoomLayout;
import dev.daniellavoie.opencabinet.project.domain.RoomLayout.Placement;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;

/**
 * The pictures printed with a Quote: the Room in 3D, then an Elevation of each Wall with Cabinets,
 * for the Client (no labels, only overall dimensions). They are vector drawings, made from the same
 * design data as the Shop Drawings, so a Quote can keep them unchanged once sent.
 */
final class QuotePicturesGenerator {
	/**
	 * How far the 3D view looks down onto the Room.
	 */
	static final double LOOK_DOWN_DEGREES = 30;

	private QuotePicturesGenerator() {
	}

	static List<View> generate(Source source) {
		var cabinets = ShopDrawingsGenerator.cabinetsInOrder(source);
		if (cabinets.isEmpty()) {
			return List.of();
		}

		var views = new ArrayList<View>();
		views.add(overview(source, cabinets));
		var walls = source.design().getWalls();
		IntStream.range(0, walls.size())
				.filter(index -> cabinets.stream().anyMatch(cabinet -> cabinet.getWallId().equals(walls.get(index).getId())))
				.forEach(index -> {
					var sheet = ShopDrawingsGenerator.elevation(source, walls.get(index), index + 1, false);
					views.add(sheet.getViews().get(0).toBuilder().title(sheet.getTitle()).build());
				});
		return views;
	}

	/**
	 * The Room from above and in front of most Cabinets, without perspective: Walls turned to the
	 * reader with their windows and doorways, then Appliances, Fillers and Cabinets with their Fronts,
	 * farthest first so nearer ones cover them.
	 */
	static View overview(Source source, List<Cabinet> cabinets) {
		var design = source.design();
		var placements = RoomLayout.layout(design.getWalls());
		var camera = Camera.facing(cabinets, placements);
		var shapes = new ArrayList<Shape>();

		for (var wall : design.getWalls()) {
			var placement = placements.get(wall.getId());
			if (!camera.faces(placement.normal())) {
				continue;
			}
			shapes.add(face(camera, placement, 0, wall.getLengthMm(), 0, 0, wall.getHeightMm(), Stroke.FACE_LIGHT));
			for (var opening : Optional.ofNullable(design.getOpenings()).orElse(List.of())) {
				if (opening.getWallId().equals(wall.getId())) {
					shapes.add(face(camera, placement, opening.getOffsetMm(), opening.getOffsetMm() + opening.getWidthMm(),
							0, opening.getElevationMm(), opening.getElevationMm() + opening.getHeightMm(), Stroke.OPENING));
				}
			}
		}

		var solids = new ArrayList<Solid>();
		for (var appliance : Optional.ofNullable(design.getAppliances()).orElse(List.of())) {
			Optional.ofNullable(placements.get(appliance.getWallId())).ifPresent(placement -> solids.add(box(camera,
					placement, appliance.getOffsetMm(), appliance.getWidthMm(), appliance.getElevationMm(),
					appliance.getHeightMm(), appliance.getDepthMm(), List.of())));
		}
		for (var filler : Optional.ofNullable(design.getFillers()).orElse(List.of())) {
			Optional.ofNullable(placements.get(filler.getWallId())).ifPresent(placement -> solids.add(box(camera,
					placement, filler.getOffsetMm(), filler.getWidthMm(), filler.getElevationMm(), filler.getHeightMm(),
					filler.getDepthMm(), List.of())));
		}
		for (var cabinet : cabinets) {
			var placement = placements.get(cabinet.getWallId());
			var fronts = ShopDrawingsGenerator.front(source, cabinet, source.template(cabinet), cabinet.getOffsetMm(),
					cabinet.getElevationMm());
			solids.add(box(camera, placement, cabinet.getOffsetMm(), cabinet.getWidthMm(), cabinet.getElevationMm(),
					cabinet.getHeightMm(), cabinet.getDepthMm(), fronts));
		}

		solids.sort(Comparator.comparingDouble(Solid::depth).reversed());
		solids.forEach(solid -> shapes.addAll(solid.shapes()));

		return View.builder()
				.title(source.language().pick("Vue d'ensemble", "Overview"))
				.shapes(shapes)
				.build();
	}

	/**
	 * A box standing against a Wall: its top, and whichever of its sides face the reader, shaded,
	 * with {@code fronts} (drawn in Elevation coordinates: along the Wall, up from the floor) on its
	 * front face.
	 */
	private static Solid box(Camera camera, Placement placement, double offset, double width, double elevation,
			double height, double depth, List<Shape> fronts) {
		var start = offset;
		var end = offset + width;
		var bottom = elevation;
		var top = elevation + height;
		var direction = placement.direction();
		var normal = placement.normal();
		var shapes = new ArrayList<Shape>();

		if (camera.faces(new RoomLayout.Point(-normal.x(), -normal.z()))) {
			shapes.add(face(camera, placement, start, end, 0, bottom, top, Stroke.FACE_DARK));
		}
		if (camera.faces(new RoomLayout.Point(-direction.x(), -direction.z()))) {
			shapes.add(end(camera, placement, start, depth, bottom, top));
		}
		if (camera.faces(direction)) {
			shapes.add(end(camera, placement, end, depth, bottom, top));
		}
		shapes.add(new Outline(List.of(camera.project(placement.at(start, 0), top),
				camera.project(placement.at(end, 0), top), camera.project(placement.at(end, depth), top),
				camera.project(placement.at(start, depth), top)), Stroke.FACE_LIGHT));
		if (camera.faces(normal)) {
			shapes.add(face(camera, placement, start, end, depth, bottom, top, Stroke.FACE_MEDIUM));
			for (var front : fronts) {
				switch (front) {
				case Outline outline -> shapes.add(new Outline(outline.points().stream()
						.map(point -> camera.project(placement.at(point.x(), depth), point.y())).toList(),
						outline.stroke()));
				case Line line -> shapes.add(new Line(camera.project(placement.at(line.from().x(), depth), line.from().y()),
						camera.project(placement.at(line.to().x(), depth), line.to().y()), line.stroke()));
				default -> {
				}
				}
			}
		}

		var centre = placement.at(offset + width / 2, depth / 2);
		return new Solid(camera.depth(centre, elevation + height / 2), shapes);
	}

	/**
	 * A vertical face parallel to its Wall, {@code out} from it.
	 */
	private static Outline face(Camera camera, Placement placement, double start, double end, double out,
			double bottom, double top, Stroke stroke) {
		return new Outline(List.of(camera.project(placement.at(start, out), bottom),
				camera.project(placement.at(end, out), bottom), camera.project(placement.at(end, out), top),
				camera.project(placement.at(start, out), top)), stroke);
	}

	/**
	 * A box's end, square to its Wall, at {@code along}.
	 */
	private static Outline end(Camera camera, Placement placement, double along, double depth, double bottom,
			double top) {
		return new Outline(List.of(camera.project(placement.at(along, 0), bottom),
				camera.project(placement.at(along, depth), bottom), camera.project(placement.at(along, depth), top),
				camera.project(placement.at(along, 0), top)), Stroke.FACE_DARK);
	}

	record Solid(double depth, List<Shape> shapes) {
	}

	/**
	 * Looks horizontally along {@code (forwardX, forwardZ)} in plan, down by
	 * {@link #LOOK_DOWN_DEGREES}. Screen x runs to the reader's right; farther and higher points
	 * appear higher.
	 */
	record Camera(double forwardX, double forwardZ, double cos, double sin) {
		/**
		 * Facing the fronts of most Cabinets: against the sum of their Walls' normals.
		 */
		static Camera facing(List<Cabinet> cabinets, Map<String, Placement> placements) {
			double x = 0;
			double z = 0;
			for (var cabinet : cabinets) {
				var normal = placements.get(cabinet.getWallId()).normal();
				x -= normal.x();
				z -= normal.z();
			}
			var length = Math.hypot(x, z);
			if (length < 1e-6) {
				var normal = placements.get(cabinets.get(0).getWallId()).normal();
				x = -normal.x();
				z = -normal.z();
				length = 1;
			}
			var radians = Math.toRadians(LOOK_DOWN_DEGREES);
			return new Camera(x / length, z / length, Math.cos(radians), Math.sin(radians));
		}

		Point project(RoomLayout.Point point, double height) {
			var across = -point.x() * forwardZ + point.z() * forwardX;
			var ahead = point.x() * forwardX + point.z() * forwardZ;
			return new Point(across, height * cos + ahead * sin);
		}

		/**
		 * Distance from the reader, for drawing farthest first.
		 */
		double depth(RoomLayout.Point point, double height) {
			return (point.x() * forwardX + point.z() * forwardZ) * cos - height * sin;
		}

		/**
		 * Whether a face with this outward normal (in plan) is turned towards the reader.
		 */
		boolean faces(RoomLayout.Point normal) {
			return normal.x() * forwardX + normal.z() * forwardZ < -1e-9;
		}
	}
}
