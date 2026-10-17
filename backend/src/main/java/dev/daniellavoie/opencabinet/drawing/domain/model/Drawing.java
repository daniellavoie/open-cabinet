package dev.daniellavoie.opencabinet.drawing.domain.model;

import java.util.List;

/**
 * A vector drawing in real-world millimetres, x to the right and y up. Renderers scale it to fit
 * their page; text and line weights keep their printed size whatever the scale.
 */
public record Drawing(String title, List<Shape> shapes) {
	/**
	 * The smallest box holding every shape, as {minX, minY, maxX, maxY}.
	 */
	public double[] bounds() {
		var bounds = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY };
		for (var shape : shapes) {
			for (var point : shape.points()) {
				bounds[0] = Math.min(bounds[0], point.x());
				bounds[1] = Math.min(bounds[1], point.y());
				bounds[2] = Math.max(bounds[2], point.x());
				bounds[3] = Math.max(bounds[3], point.y());
			}
		}
		return shapes.isEmpty() ? new double[] { 0, 0, 1, 1 } : bounds;
	}
}
