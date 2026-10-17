package dev.daniellavoie.opencabinet.drawing.module.adapter.svg;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.drawing.domain.DrawingCanvas;
import dev.daniellavoie.opencabinet.drawing.domain.DrawingPainter;
import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

/**
 * Renders a Drawing as a standalone SVG document. Colours follow the app's light palette; the page
 * shows them on white whatever the theme, like paper.
 */
public final class SvgCanvas implements DrawingCanvas {
	static final double WIDTH = 1000;

	private final StringBuilder body = new StringBuilder();

	private SvgCanvas() {
	}

	public static String render(Drawing drawing) {
		var height = DrawingPainter.heightFor(drawing, WIDTH);
		var canvas = new SvgCanvas();
		DrawingPainter.paint(drawing, canvas, 0, 0, WIDTH, height);
		return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + number(WIDTH) + " " + number(height)
				+ "\" font-family=\"system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif\">"
				+ "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>" + canvas.body + "</svg>";
	}

	@Override
	public void polygon(List<Point> points, Stroke stroke, Fill fill) {
		body.append("<polygon points=\"")
				.append(points.stream().map(point -> number(point.x()) + "," + number(point.y()))
						.collect(Collectors.joining(" ")))
				.append("\" fill=\"").append(fill(fill)).append('"').append(stroke(stroke)).append("/>");
	}

	@Override
	public void line(Point from, Point to, Stroke stroke) {
		body.append("<line x1=\"").append(number(from.x())).append("\" y1=\"").append(number(from.y()))
				.append("\" x2=\"").append(number(to.x())).append("\" y2=\"").append(number(to.y())).append('"')
				.append(stroke(stroke)).append("/>");
	}

	@Override
	public void text(Point at, String text, TextStyle style, double angleDegrees) {
		body.append("<text x=\"").append(number(at.x())).append("\" y=\"").append(number(at.y()))
				.append("\" text-anchor=\"middle\" dominant-baseline=\"central\" font-size=\"").append(size(style))
				.append('"').append(style == TextStyle.LABEL ? " font-weight=\"600\"" : "")
				.append(" fill=\"").append(style == TextStyle.LABEL ? "#212529" : "#495057").append('"');
		if (angleDegrees != 0) {
			body.append(" transform=\"rotate(").append(number(angleDegrees)).append(' ').append(number(at.x()))
					.append(' ').append(number(at.y())).append(")\"");
		}
		body.append('>').append(escape(text)).append("</text>");
	}

	static String fill(Fill fill) {
		return switch (fill) {
		case NONE -> "none";
		case WHITE -> "#ffffff";
		case LIGHT -> "#f1f3f5";
		case MEDIUM -> "#ced4da";
		case DARK -> "#495057";
		case WALL -> "#adb5bd";
		case WINDOW -> "#dbe7fb";
		case APPLIANCE -> "#e9ecef";
		};
	}

	private static String stroke(Stroke stroke) {
		return switch (stroke) {
		case NONE -> " stroke=\"none\"";
		case WALL -> " stroke=\"#212529\" stroke-width=\"3\"";
		case OUTLINE -> " stroke=\"#212529\" stroke-width=\"1.6\" stroke-linejoin=\"round\"";
		case THIN -> " stroke=\"#495057\" stroke-width=\"0.8\"";
		case HIDDEN -> " stroke=\"#495057\" stroke-width=\"0.8\" stroke-dasharray=\"6 4\"";
		};
	}

	private static String size(TextStyle style) {
		return switch (style) {
		case LABEL -> "17";
		case SMALL -> "13";
		case DIMENSION -> "14";
		};
	}

	private static String number(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
