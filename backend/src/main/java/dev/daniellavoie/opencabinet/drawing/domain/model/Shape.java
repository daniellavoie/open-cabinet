package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

/**
 * Something drawn: a closed polygon, an open line, a text, or a dimension between two points.
 */
public sealed interface Shape {
	List<Point> points();

	record Polygon(List<Point> points, Stroke stroke, Fill fill) implements Shape {
	}

	record Line(Point from, Point to, Stroke stroke) implements Shape {
		@Override
		public List<Point> points() {
			return List.of(from, to);
		}
	}

	/**
	 * Text centred on {@code at}, turned {@code angleDegrees} counter-clockwise.
	 */
	record Text(Point at, String text, TextStyle style, double angleDegrees) implements Shape {
		@Override
		public List<Point> points() {
			return List.of(at);
		}
	}

	/**
	 * A dimension line measuring from {@code from} to {@code to}, drawn {@code offsetMm} to the left of
	 * that direction with extension lines back to both points.
	 */
	record Dimension(Point from, Point to, double offsetMm, String label) implements Shape {
		@Override
		public List<Point> points() {
			var length = Math.hypot(to.x() - from.x(), to.y() - from.y());
			if (length == 0) {
				return List.of(from, to);
			}
			var nx = -(to.y() - from.y()) / length * offsetMm;
			var ny = (to.x() - from.x()) / length * offsetMm;
			return List.of(from, to, new Point(from.x() + nx, from.y() + ny), new Point(to.x() + nx, to.y() + ny));
		}
	}
}
