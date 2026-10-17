package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

/**
 * Shorthands for building drawings.
 */
final class Shapes {
	private Shapes() {
	}

	static Shape.Polygon rect(double x, double y, double width, double height, Stroke stroke, Fill fill) {
		return new Shape.Polygon(List.of(new Point(x, y), new Point(x + width, y), new Point(x + width, y + height),
				new Point(x, y + height)), stroke, fill);
	}

	static Shape.Line line(double x1, double y1, double x2, double y2, Stroke stroke) {
		return new Shape.Line(new Point(x1, y1), new Point(x2, y2), stroke);
	}

	static Shape.Text text(double x, double y, String text, TextStyle style) {
		return new Shape.Text(new Point(x, y), text, style, 0);
	}

	/**
	 * A horizontal dimension from x1 to x2 at height y, drawn {@code offsetMm} above (or below when
	 * negative).
	 */
	static Shape.Dimension horizontal(double x1, double x2, double y, double offsetMm, String label) {
		return new Shape.Dimension(new Point(x1, y), new Point(x2, y), offsetMm, label);
	}

	/**
	 * A vertical dimension from y1 to y2 at x, drawn {@code offsetMm} to the left (or right when
	 * negative).
	 */
	static Shape.Dimension vertical(double x, double y1, double y2, double offsetMm, String label) {
		return new Shape.Dimension(new Point(x, y1), new Point(x, y2), offsetMm, label);
	}
}
