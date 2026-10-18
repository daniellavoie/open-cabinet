package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;

import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Line;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;

/**
 * Draws Views on a PDF canvas at a given scale: outlines and lines by stroke, dimension lines with
 * architectural ticks, and labels over white so lines don't cross them. Shared by the Shop Drawings
 * and the Quote PDF.
 */
public class PdfViewPainter {
	/**
	 * Distance between dimension lines, and from the first one to what it measures.
	 */
	static final float DIMENSION_STEP = 14;
	static final float TEXT_SIZE = 7;
	public static final float POINTS_PER_MM = 72 / 25.4f;

	static final Color INK = Color.BLACK;
	static final Color MUTED = new Color(0x49, 0x50, 0x57);
	private static final Color WALL_FILL = new Color(0xce, 0xd4, 0xda);
	private static final Color FACE_LIGHT = new Color(0xf8, 0xf9, 0xfa);
	private static final Color FACE_MEDIUM = new Color(0xe9, 0xec, 0xef);
	private static final Color FACE_DARK = new Color(0xce, 0xd4, 0xda);

	private final BaseFont regular;
	private final BaseFont bold;

	public PdfViewPainter() {
		try {
			regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		} catch (Exception exception) {
			throw new IllegalStateException("could not load the PDF fonts", exception);
		}
	}

	public BaseFont regular() {
		return regular;
	}

	public BaseFont bold() {
		return bold;
	}

	static boolean fits(List<ViewBox> boxes, double pointsPerMm, double width, double height) {
		return boxes.stream().mapToDouble(box -> box.width(pointsPerMm)).sum() <= width
				&& boxes.stream().allMatch(box -> box.height(pointsPerMm) <= height);
	}

	/**
	 * The scale that just fits, for Rooms too big for every standard scale.
	 */
	public static double fit(List<ViewBox> boxes, double width, double height) {
		var paddingWidth = boxes.stream().mapToDouble(box -> 2 * box.padding()).sum();
		var modelWidth = boxes.stream().mapToDouble(ViewBox::modelWidth).sum();
		var widest = boxes.stream().mapToDouble(box -> (height - 2 * box.padding()) / Math.max(box.modelHeight(), 1))
				.min().orElse(1);
		return Math.max(0.001, Math.min((width - paddingWidth) / Math.max(modelWidth, 1), widest));
	}

	/**
	 * A View's extent in millimetres, and the room its dimension lines need around it on paper.
	 */
	public record ViewBox(double minX, double minY, double modelWidth, double modelHeight, double padding) {
		public static ViewBox of(View view) {
			var points = new ArrayList<Point>();
			var levels = 0;
			for (var shape : view.getShapes()) {
				switch (shape) {
				case Outline outline -> points.addAll(outline.points());
				case Line line -> {
					points.add(line.from());
					points.add(line.to());
				}
				case Label label -> points.add(label.at());
				case Dimension dimension -> {
					points.add(dimension.from());
					points.add(dimension.to());
					levels = Math.max(levels, dimension.level());
				}
				}
			}
			var minX = points.stream().mapToDouble(Point::x).min().orElse(0);
			var maxX = points.stream().mapToDouble(Point::x).max().orElse(0);
			var minY = points.stream().mapToDouble(Point::y).min().orElse(0);
			var maxY = points.stream().mapToDouble(Point::y).max().orElse(0);
			return new ViewBox(minX, minY, maxX - minX, maxY - minY, levels * DIMENSION_STEP + 12);
		}

		public double width(double pointsPerMm) {
			return modelWidth * pointsPerMm + 2 * padding;
		}

		public double height(double pointsPerMm) {
			return modelHeight * pointsPerMm + 2 * padding;
		}
	}

	/**
	 * Draws a View with its box's bottom-left corner at {@code (x, y)}.
	 */
	public void draw(PdfContentByte canvas, View view, ViewBox box, double pointsPerMm, float x, float y) {
		var originX = x + box.padding();
		var originY = y + box.padding();
		Function<Point, float[]> page = point -> new float[] {
				(float) (originX + (point.x() - box.minX()) * pointsPerMm),
				(float) (originY + (point.y() - box.minY()) * pointsPerMm) };

		var labels = new ArrayList<Label>();
		var dimensions = new ArrayList<Dimension>();
		for (var shape : view.getShapes()) {
			switch (shape) {
			case Outline outline -> {
				var points = outline.points().stream().map(page).toList();
				canvas.saveState();
				stroke(canvas, outline.stroke());
				canvas.moveTo(points.get(0)[0], points.get(0)[1]);
				points.stream().skip(1).forEach(point -> canvas.lineTo(point[0], point[1]));
				canvas.closePath();
				switch (outline.stroke()) {
				case WALL, OPENING, FACE_LIGHT, FACE_MEDIUM, FACE_DARK -> canvas.fillStroke();
				default -> canvas.stroke();
				}
				canvas.restoreState();
			}
			case Line line -> {
				var from = page.apply(line.from());
				var to = page.apply(line.to());
				canvas.saveState();
				stroke(canvas, line.stroke());
				canvas.moveTo(from[0], from[1]);
				canvas.lineTo(to[0], to[1]);
				canvas.stroke();
				canvas.restoreState();
			}
			case Label label -> labels.add(label);
			case Dimension dimension -> dimensions.add(dimension);
			}
		}

		dimensions.forEach(dimension -> dimension(canvas, page.apply(dimension.from()), page.apply(dimension.to()),
				dimension.level(), dimension.text()));
		labels.forEach(label -> {
			var at = page.apply(label.at());
			boxedText(canvas, label.text(), at[0], at[1], 0);
		});
	}

	private static void stroke(PdfContentByte canvas, Stroke stroke) {
		switch (stroke) {
		case OUTLINE -> {
			canvas.setLineWidth(1f);
			canvas.setColorStroke(INK);
		}
		case THIN -> {
			canvas.setLineWidth(0.5f);
			canvas.setColorStroke(INK);
		}
		case HIDDEN -> {
			canvas.setLineWidth(0.5f);
			canvas.setColorStroke(MUTED);
			canvas.setLineDash(3f, 2f, 0f);
		}
		case WALL -> {
			canvas.setLineWidth(0.5f);
			canvas.setColorStroke(INK);
			canvas.setColorFill(WALL_FILL);
		}
		case OPENING -> {
			canvas.setLineWidth(0.5f);
			canvas.setColorStroke(INK);
			canvas.setColorFill(Color.WHITE);
		}
		case FACE_LIGHT, FACE_MEDIUM, FACE_DARK -> {
			canvas.setLineWidth(0.5f);
			canvas.setColorStroke(INK);
			canvas.setColorFill(stroke == Stroke.FACE_LIGHT ? FACE_LIGHT
					: stroke == Stroke.FACE_MEDIUM ? FACE_MEDIUM : FACE_DARK);
		}
		}
	}

	/**
	 * Extension lines, a dimension line with architectural ticks, and the length written on it,
	 * {@code level} steps to the left of {@code from → to}.
	 */
	private void dimension(PdfContentByte canvas, float[] from, float[] to, int level, String text) {
		var length = Math.hypot(to[0] - from[0], to[1] - from[1]);
		if (length < 0.5) {
			return;
		}
		var ux = (float) ((to[0] - from[0]) / length);
		var uy = (float) ((to[1] - from[1]) / length);
		var nx = -uy;
		var ny = ux;
		var offset = level * DIMENSION_STEP;

		canvas.saveState();
		canvas.setLineWidth(0.4f);
		canvas.setColorStroke(MUTED);
		for (var end : List.of(from, to)) {
			canvas.moveTo(end[0] + nx * 2, end[1] + ny * 2);
			canvas.lineTo(end[0] + nx * (offset + 3), end[1] + ny * (offset + 3));
			var tickX = end[0] + nx * offset;
			var tickY = end[1] + ny * offset;
			canvas.moveTo(tickX - (ux + nx) * 2.5f, tickY - (uy + ny) * 2.5f);
			canvas.lineTo(tickX + (ux + nx) * 2.5f, tickY + (uy + ny) * 2.5f);
		}
		canvas.moveTo(from[0] + nx * offset, from[1] + ny * offset);
		canvas.lineTo(to[0] + nx * offset, to[1] + ny * offset);
		canvas.stroke();
		canvas.restoreState();

		var angle = Math.toDegrees(Math.atan2(uy, ux));
		if (angle > 90.5 || angle <= -89.5) {
			angle += 180;
		}
		boxedText(canvas, text, (from[0] + to[0]) / 2 + nx * offset, (from[1] + to[1]) / 2 + ny * offset,
				(float) angle);
	}

	/**
	 * Text centred on a point, over a white box so lines behind it don't cross it.
	 */
	private void boxedText(PdfContentByte canvas, String text, float x, float y, float angle) {
		var width = regular.getWidthPoint(text, TEXT_SIZE) + 3;
		var height = TEXT_SIZE + 2;
		canvas.saveState();
		var radians = Math.toRadians(angle);
		var cos = (float) Math.cos(radians);
		var sin = (float) Math.sin(radians);
		canvas.concatCTM(cos, sin, -sin, cos, x, y);
		canvas.setColorFill(Color.WHITE);
		canvas.rectangle(-width / 2, -height / 2, width, height);
		canvas.fill();
		canvas.restoreState();
		// The baseline sits a third of the text's height below its centre, along the text's own axis.
		var drop = TEXT_SIZE * 0.35f;
		text(canvas, regular, TEXT_SIZE, INK, text, x + sin * drop, y - cos * drop, angle, Element.ALIGN_CENTER);
	}

	public static void text(PdfContentByte canvas, BaseFont font, float size, Color color, String text, float x,
			float y, float angle, int alignment) {
		canvas.saveState();
		canvas.beginText();
		canvas.setFontAndSize(font, size);
		canvas.setColorFill(color);
		canvas.showTextAligned(alignment, text, x, y, angle);
		canvas.endText();
		canvas.restoreState();
	}
}
