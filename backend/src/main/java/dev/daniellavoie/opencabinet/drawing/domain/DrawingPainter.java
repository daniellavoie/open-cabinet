package dev.daniellavoie.opencabinet.drawing.domain;

import java.util.List;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

/**
 * Paints a Drawing onto a canvas, scaled to fit a box and centred in it, with y turned down.
 * Dimensions become extension lines, a dimension line with slashes at both ends and a label.
 */
public final class DrawingPainter {
	/**
	 * Room kept around the drawing for labels, as a share of the box's smaller side.
	 */
	static final double MARGIN = 0.06;
	static final double TICK = 4;
	static final double EXTENSION_GAP = 2;
	static final double LABEL_LIFT = 7;

	private final double scale;
	private final double offsetX;
	private final double offsetY;

	private DrawingPainter(double scale, double offsetX, double offsetY) {
		this.scale = scale;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	/**
	 * Paints {@code drawing} into the box at ({@code x}, {@code y}) sized {@code width} by
	 * {@code height}, page units with y down.
	 */
	public static void paint(Drawing drawing, DrawingCanvas canvas, double x, double y, double width, double height) {
		var bounds = drawing.bounds();
		var margin = Math.min(width, height) * MARGIN;
		var drawingWidth = Math.max(bounds[2] - bounds[0], 1);
		var drawingHeight = Math.max(bounds[3] - bounds[1], 1);
		var scale = Math.min((width - 2 * margin) / drawingWidth, (height - 2 * margin) / drawingHeight);
		var offsetX = x + (width - drawingWidth * scale) / 2 - bounds[0] * scale;
		var offsetY = y + (height + drawingHeight * scale) / 2 + bounds[1] * scale;
		new DrawingPainter(scale, offsetX, offsetY).paint(drawing, canvas);
	}

	/**
	 * The page height that keeps {@code drawing}'s proportions at {@code width}.
	 */
	public static double heightFor(Drawing drawing, double width) {
		var bounds = drawing.bounds();
		var ratio = Math.max(bounds[3] - bounds[1], 1) / Math.max(bounds[2] - bounds[0], 1);
		return Math.max(width * ratio, width * 0.2) + 2 * width * MARGIN;
	}

	private void paint(Drawing drawing, DrawingCanvas canvas) {
		for (var shape : drawing.shapes()) {
			switch (shape) {
			case Shape.Polygon polygon -> canvas.polygon(polygon.points().stream().map(this::page).toList(),
					polygon.stroke(), polygon.fill());
			case Shape.Line line -> canvas.line(page(line.from()), page(line.to()), line.stroke());
			case Shape.Text text -> canvas.text(page(text.at()), text.text(), text.style(), -text.angleDegrees());
			case Shape.Dimension dimension -> dimension(dimension, canvas);
			}
		}
	}

	private void dimension(Shape.Dimension dimension, DrawingCanvas canvas) {
		var points = dimension.points();
		if (points.size() < 4) {
			return;
		}
		var from = page(points.get(0));
		var to = page(points.get(1));
		var lineFrom = page(points.get(2));
		var lineTo = page(points.get(3));

		var dx = lineTo.x() - lineFrom.x();
		var dy = lineTo.y() - lineFrom.y();
		var length = Math.hypot(dx, dy);
		if (length < 0.5) {
			return;
		}
		var ux = dx / length;
		var uy = dy / length;
		// Unit vector from the measured points towards the dimension line.
		var ox = lineFrom.x() - from.x();
		var oy = lineFrom.y() - from.y();
		var offset = Math.hypot(ox, oy);
		var nx = offset == 0 ? -uy : ox / offset;
		var ny = offset == 0 ? ux : oy / offset;

		canvas.line(along(from, nx, ny, EXTENSION_GAP), along(lineFrom, nx, ny, TICK), Stroke.THIN);
		canvas.line(along(to, nx, ny, EXTENSION_GAP), along(lineTo, nx, ny, TICK), Stroke.THIN);
		canvas.line(lineFrom, lineTo, Stroke.THIN);
		for (var end : List.of(lineFrom, lineTo)) {
			canvas.line(new Point(end.x() - (ux + nx) * TICK * 0.7, end.y() - (uy + ny) * TICK * 0.7),
					new Point(end.x() + (ux + nx) * TICK * 0.7, end.y() + (uy + ny) * TICK * 0.7), Stroke.OUTLINE);
		}

		var angle = Math.toDegrees(Math.atan2(uy, ux));
		// Labels read from the bottom or from the right, as on a drafting table.
		if (angle >= 90) {
			angle -= 180;
		} else if (angle < -90) {
			angle += 180;
		}
		var middle = new Point((lineFrom.x() + lineTo.x()) / 2 + nx * LABEL_LIFT,
				(lineFrom.y() + lineTo.y()) / 2 + ny * LABEL_LIFT);
		canvas.text(middle, dimension.label(), TextStyle.DIMENSION, angle);
	}

	private static Point along(Point point, double nx, double ny, double distance) {
		return new Point(point.x() + nx * distance, point.y() + ny * distance);
	}

	private Point page(Point point) {
		return new Point(offsetX + point.x() * scale, offsetY - point.y() * scale);
	}
}
