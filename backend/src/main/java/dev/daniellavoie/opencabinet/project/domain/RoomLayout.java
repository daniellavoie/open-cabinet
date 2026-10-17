package dev.daniellavoie.opencabinet.project.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.daniellavoie.opencabinet.project.domain.model.Wall;

/**
 * Where each Wall sits in plan view, in millimetres, and the footprints of what stands against it.
 * Same layout as {@code room-layout.ts}: the first Wall starts at the origin and runs along +x;
 * each next Wall turns by the previous Wall's {@code angleToNextDegrees}. Things stand on the
 * Wall's left, the side {@code normal} points to.
 */
final class RoomLayout {
	record Point(double x, double z) {
	}

	record Placement(Point start, Point direction) {
		Point normal() {
			return new Point(-direction.z(), direction.x());
		}

		/**
		 * The plan-view rectangle spanning {@code alongStart..alongEnd} along the Wall and
		 * {@code outStart..outEnd} out from it.
		 */
		List<Point> rectangle(double alongStart, double alongEnd, double outStart, double outEnd) {
			return List.of(at(alongStart, outStart), at(alongEnd, outStart), at(alongEnd, outEnd),
					at(alongStart, outEnd));
		}

		private Point at(double along, double out) {
			var normal = normal();
			return new Point(start.x() + direction.x() * along + normal.x() * out,
					start.z() + direction.z() * along + normal.z() * out);
		}
	}

	private RoomLayout() {
	}

	static Map<String, Placement> layout(List<Wall> walls) {
		var placements = new HashMap<String, Placement>();
		double x = 0;
		double z = 0;
		double angle = 0;

		for (var wall : walls) {
			var direction = new Point(Math.cos(angle), Math.sin(angle));
			placements.putIfAbsent(wall.getId(), new Placement(new Point(x, z), direction));
			x += direction.x() * wall.getLengthMm();
			z += direction.z() * wall.getLengthMm();
			angle += Math.toRadians(wall.getAngleToNextDegrees());
		}

		return placements;
	}

	/**
	 * Whether two convex polygons overlap by more than {@code toleranceMm} (separating axis test),
	 * so polygons that only touch do not.
	 */
	static boolean overlap(List<Point> first, List<Point> second, double toleranceMm) {
		return separatedOnNoAxisOf(first, first, second, toleranceMm)
				&& separatedOnNoAxisOf(second, first, second, toleranceMm);
	}

	private static boolean separatedOnNoAxisOf(List<Point> edges, List<Point> first, List<Point> second,
			double toleranceMm) {
		for (int index = 0; index < edges.size(); index++) {
			var from = edges.get(index);
			var to = edges.get((index + 1) % edges.size());
			var length = Math.hypot(to.x() - from.x(), to.z() - from.z());
			if (length == 0) {
				continue;
			}
			var axis = new Point(-(to.z() - from.z()) / length, (to.x() - from.x()) / length);
			var firstRange = project(first, axis);
			var secondRange = project(second, axis);
			if (Math.min(firstRange[1], secondRange[1]) - Math.max(firstRange[0], secondRange[0]) <= toleranceMm) {
				return false;
			}
		}
		return true;
	}

	private static double[] project(List<Point> polygon, Point axis) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (var point : polygon) {
			var value = point.x() * axis.x() + point.z() * axis.z();
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		return new double[] { min, max };
	}
}
