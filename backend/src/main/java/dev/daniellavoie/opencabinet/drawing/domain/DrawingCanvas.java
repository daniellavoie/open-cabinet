package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

/**
 * Where a {@link DrawingPainter} paints, in page units with y down: an SVG document or a PDF page.
 */
public interface DrawingCanvas {
	void polygon(List<Point> points, Stroke stroke, Fill fill);

	void line(Point from, Point to, Stroke stroke);

	/**
	 * Text centred on {@code at}, turned {@code angleDegrees} clockwise on the page.
	 */
	void text(Point at, String text, TextStyle style, double angleDegrees);
}
