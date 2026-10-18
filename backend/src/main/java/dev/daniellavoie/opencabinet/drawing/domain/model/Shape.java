package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

/**
 * Something drawn in a View, in millimetres: x to the right, y up. Text and dimension spacing are
 * sized on paper, so they read the same at any scale.
 */
public sealed interface Shape {
	record Point(double x, double y) {
	}

	/**
	 * A closed outline, filled or not depending on its stroke.
	 */
	record Outline(List<Point> points, Stroke stroke) implements Shape {
		public static Outline box(double x, double y, double width, double height, Stroke stroke) {
			return new Outline(List.of(new Point(x, y), new Point(x + width, y), new Point(x + width, y + height),
					new Point(x, y + height)), stroke);
		}
	}

	record Line(Point from, Point to, Stroke stroke) implements Shape {
	}

	/**
	 * Text centred on a point.
	 */
	record Label(Point at, String text) implements Shape {
	}

	/**
	 * The distance between two points, written on a dimension line drawn {@code level} steps to the
	 * left of {@code from → to} (level 1 is closest).
	 */
	record Dimension(Point from, Point to, int level, String text) implements Shape {
	}
}
