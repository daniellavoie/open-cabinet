package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.project.domain.RoomLayout;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;

/**
 * The Room in 3D, seen from inside it and from above, as flat-shaded faces drawn back to front: the
 * Walls with their Openings, then Appliances, Fillers and Cabinets with their Fronts. The viewer
 * stands where the Cabinets face.
 */
public final class IsometricView {
	static final double ELEVATION_DEGREES = 30;
	static final double TURN_DEGREES = 30;

	private IsometricView() {
	}

	/**
	 * A point in the Room: {@code x} and {@code z} in plan, {@code y} up.
	 */
	record Point3(double x, double y, double z) {
	}

	/**
	 * A solid to draw: its faces with their outward normals, and its distance for ordering.
	 */
	private record Solid(double depth, List<Face> faces, List<Shape> details) {
	}

	private record Face(List<Point3> corners, Point3 normal, Fill fill) {
	}

	public static Drawing draw(DrawingContext context) {
		var placements = RoomLayout.layout(context.walls());
		var camera = Camera.facing(context, placements);
		var shapes = new ArrayList<Shape>();

		for (var wall : context.walls()) {
			var placement = placements.get(wall.getId());
			var face = wallFace(placement, 0, wall.getLengthMm(), 0, wall.getHeightMm(), 0);
			if (camera.sees(normal(placement))) {
				shapes.add(camera.polygon(face, Stroke.THIN, Fill.LIGHT));
				for (var opening : context.openings()) {
					if (opening.getWallId().equals(wall.getId())) {
						shapes.add(camera.polygon(wallFace(placement, opening.getOffsetMm(),
								opening.getOffsetMm() + opening.getWidthMm(), opening.getElevationMm(),
								opening.getElevationMm() + opening.getHeightMm(), 1), Stroke.THIN,
								opening.getKind() == OpeningKind.WINDOW ? Fill.WINDOW : Fill.WHITE));
					}
				}
			}
		}

		var solids = new ArrayList<Solid>();
		for (var appliance : context.appliances()) {
			var placement = placements.get(appliance.getWallId());
			if (placement != null) {
				solids.add(box(camera, placement, appliance.getOffsetMm(), appliance.getWidthMm(), 0,
						appliance.getDepthMm(), appliance.getElevationMm(), appliance.getHeightMm(), Fill.APPLIANCE,
						true, List.of()));
			}
		}
		for (var filler : context.fillers()) {
			var placement = placements.get(filler.getWallId());
			if (placement != null) {
				solids.add(box(camera, placement, filler.getOffsetMm(), filler.getWidthMm(),
						filler.getDepthMm() - ShopDrawings.FRONT_THICKNESS_MM, filler.getDepthMm(),
						filler.getElevationMm(), filler.getHeightMm(), Fill.LIGHT, true, List.of()));
			}
		}
		for (var cabinet : context.cabinets()) {
			var placement = placements.get(cabinet.getWallId());
			var template = context.templates().get(cabinet.getTemplateCode());
			if (placement == null || template == null) {
				continue;
			}
			// The front, drawn flat as in an Elevation, then laid onto the Cabinet's face.
			var front = new ArrayList<Shape>();
			CabinetFront.draw(front, cabinet, template, 0, 0, context, false);
			var details = front.stream()
					.map(shape -> onFace(camera, placement, cabinet.getOffsetMm(), cabinet.getDepthMm(),
							cabinet.getElevationMm(), shape))
					.filter(shape -> shape != null)
					.toList();
			solids.add(box(camera, placement, cabinet.getOffsetMm(), cabinet.getWidthMm(), 0, cabinet.getDepthMm(),
					cabinet.getElevationMm(), cabinet.getHeightMm(), Fill.WHITE, false, details));
		}

		solids.sort(Comparator.comparingDouble(Solid::depth));
		for (var solid : solids) {
			solid.faces().stream()
					.filter(face -> camera.sees(face.normal()))
					.forEach(face -> shapes.add(camera.polygon(face.corners(), Stroke.OUTLINE,
							camera.shade(face.fill(), face.normal()))));
			shapes.addAll(solid.details());
		}

		return new Drawing(context.language().pick("Vue 3D", "3D view"), shapes);
	}

	/**
	 * A box standing against a Wall. Its front face is left out when {@code withFront} is false:
	 * the Cabinet's own front is laid there instead.
	 */
	private static Solid box(Camera camera, RoomLayout.Placement placement, double offset, double width,
			double outStart, double outEnd, double bottom, double height, Fill fill, boolean withFront,
			List<Shape> details) {
		var footprint = placement.rectangle(offset, offset + width, outStart, outEnd);
		var top = bottom + height;
		var faces = new ArrayList<Face>();
		faces.add(new Face(footprint.stream().map(point -> new Point3(point.x(), top, point.z())).toList(),
				new Point3(0, 1, 0), fill));
		var centre = new Point3(footprint.stream().mapToDouble(RoomLayout.Point::x).average().orElse(0), 0,
				footprint.stream().mapToDouble(RoomLayout.Point::z).average().orElse(0));
		for (int index = 0; index < footprint.size(); index++) {
			// The rectangle's third edge (index 2) runs along the face at outEnd: the front.
			if (index == 2 && !withFront) {
				continue;
			}
			var from = footprint.get(index);
			var to = footprint.get((index + 1) % footprint.size());
			var middleX = (from.x() + to.x()) / 2;
			var middleZ = (from.z() + to.z()) / 2;
			var length = Math.hypot(middleX - centre.x(), middleZ - centre.z());
			var normal = new Point3((middleX - centre.x()) / length, 0, (middleZ - centre.z()) / length);
			faces.add(new Face(List.of(new Point3(from.x(), bottom, from.z()), new Point3(to.x(), bottom, to.z()),
					new Point3(to.x(), top, to.z()), new Point3(from.x(), top, from.z())), normal, fill));
		}
		var middle = placement.at(offset + width / 2, (outStart + outEnd) / 2);
		return new Solid(camera.depth(new Point3(middle.x(), bottom + height / 2, middle.z())), faces, details);
	}

	/**
	 * A shape drawn on a Cabinet's front, laid onto the face {@code depth} out from the Wall.
	 */
	private static Shape onFace(Camera camera, RoomLayout.Placement placement, double offset, double depth,
			double elevation, Shape shape) {
		return switch (shape) {
		case Shape.Polygon polygon -> new Shape.Polygon(polygon.points().stream()
				.map(point -> camera.project(face(placement, offset, depth, elevation, point)))
				.toList(), polygon.stroke() == Stroke.OUTLINE ? Stroke.THIN : polygon.stroke(),
				camera.shade(polygon.fill(), normal(placement)));
		case Shape.Line line -> new Shape.Line(camera.project(face(placement, offset, depth, elevation, line.from())),
				camera.project(face(placement, offset, depth, elevation, line.to())), line.stroke());
		default -> null;
		};
	}

	private static Point3 face(RoomLayout.Placement placement, double offset, double depth, double elevation,
			Point point) {
		var at = placement.at(offset + point.x(), depth);
		return new Point3(at.x(), elevation + point.y(), at.z());
	}

	private static List<Point3> wallFace(RoomLayout.Placement placement, double alongStart, double alongEnd,
			double bottom, double top, double out) {
		var start = placement.at(alongStart, out);
		var end = placement.at(alongEnd, out);
		return List.of(new Point3(start.x(), bottom, start.z()), new Point3(end.x(), bottom, end.z()),
				new Point3(end.x(), top, end.z()), new Point3(start.x(), top, start.z()));
	}

	private static Point3 normal(RoomLayout.Placement placement) {
		var normal = placement.normal();
		return new Point3(normal.x(), 0, normal.z());
	}

	/**
	 * Where the viewer stands: {@code vx}, {@code vz} point from the Room towards them in plan.
	 */
	private record Camera(double vx, double vz, double cos, double sin) {
		static Camera facing(DrawingContext context, java.util.Map<String, RoomLayout.Placement> placements) {
			var x = 0d;
			var z = 0d;
			var length = 0d;
			for (var wall : context.walls()) {
				var normal = placements.get(wall.getId()).normal();
				x += normal.x() * wall.getLengthMm();
				z += normal.z() * wall.getLengthMm();
				length += wall.getLengthMm();
			}
			// Walls facing different ways already show their Cabinets at an angle; a straight run is
			// turned so its Cabinets show their sides.
			var straight = Math.hypot(x, z) > 0.95 * length;
			var angle = (Math.hypot(x, z) < 1 ? Math.PI / 2 : Math.atan2(z, x))
					- (straight ? Math.toRadians(TURN_DEGREES) : 0);
			var elevation = Math.toRadians(ELEVATION_DEGREES);
			return new Camera(Math.cos(angle), Math.sin(angle), Math.cos(elevation), Math.sin(elevation));
		}

		Point project(Point3 point) {
			var across = point.x() * vz - point.z() * vx;
			var towards = point.x() * vx + point.z() * vz;
			return new Point(across, point.y() * cos - towards * sin);
		}

		double depth(Point3 point) {
			return (point.x() * vx + point.z() * vz) * cos + point.y() * sin;
		}

		boolean sees(Point3 normal) {
			return normal.x() * vx * cos + normal.y() * sin + normal.z() * vz * cos > 1e-6;
		}

		/**
		 * Lights faces from the viewer's upper left: tops brightest, faces turned away darker.
		 */
		Fill shade(Fill fill, Point3 normal) {
			if (fill != Fill.WHITE || normal.y() > 0.5) {
				return fill;
			}
			var facing = normal.x() * vx + normal.z() * vz;
			return facing > 0.8 ? Fill.WHITE : facing > 0.3 ? Fill.LIGHT : Fill.MEDIUM;
		}

		Shape.Polygon polygon(List<Point3> corners, Stroke stroke, Fill fill) {
			return new Shape.Polygon(corners.stream().map(this::project).toList(), stroke, fill);
		}
	}
}
