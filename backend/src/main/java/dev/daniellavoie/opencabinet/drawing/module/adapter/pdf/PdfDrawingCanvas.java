package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import java.awt.Color;
import java.util.List;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;

import dev.daniellavoie.opencabinet.drawing.domain.DrawingCanvas;
import dev.daniellavoie.opencabinet.drawing.domain.DrawingPainter;
import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Fill;
import dev.daniellavoie.opencabinet.drawing.domain.model.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.TextStyle;

/**
 * Paints Drawings onto a PDF page with the same palette as the SVG ones. Shared by the Shop
 * Drawings, the Quote and the printed outputs.
 */
public final class PdfDrawingCanvas implements DrawingCanvas {
	private static final BaseFont REGULAR = font(BaseFont.HELVETICA);
	private static final BaseFont BOLD = font(BaseFont.HELVETICA_BOLD);

	private final PdfContentByte content;
	private final float pageHeight;

	private PdfDrawingCanvas(PdfContentByte content, float pageHeight) {
		this.content = content;
		this.pageHeight = pageHeight;
	}

	/**
	 * Paints {@code drawing} into a box given from the page's top-left corner, in points.
	 */
	public static void paint(Drawing drawing, PdfContentByte content, float pageHeight, float x, float top,
			float width, float height) {
		DrawingPainter.paint(drawing, new PdfDrawingCanvas(content, pageHeight), x, top, width, height);
	}

	@Override
	public void polygon(List<Point> points, Stroke stroke, Fill fill) {
		if (points.isEmpty()) {
			return;
		}
		content.saveState();
		content.moveTo((float) points.get(0).x(), y(points.get(0).y()));
		points.stream().skip(1).forEach(point -> content.lineTo((float) point.x(), y(point.y())));
		content.closePath();
		var filled = fill != Fill.NONE;
		var stroked = stroke != Stroke.NONE;
		if (filled) {
			content.setColorFill(color(fill));
		}
		if (stroked) {
			applyStroke(stroke);
		}
		if (filled && stroked) {
			content.fillStroke();
		} else if (filled) {
			content.fill();
		} else if (stroked) {
			content.stroke();
		} else {
			content.newPath();
		}
		content.restoreState();
	}

	@Override
	public void line(Point from, Point to, Stroke stroke) {
		if (stroke == Stroke.NONE) {
			return;
		}
		content.saveState();
		applyStroke(stroke);
		content.moveTo((float) from.x(), y(from.y()));
		content.lineTo((float) to.x(), y(to.y()));
		content.stroke();
		content.restoreState();
	}

	@Override
	public void text(Point at, String text, TextStyle style, double angleDegrees) {
		var size = switch (style) {
		case LABEL -> 8f;
		case SMALL -> 6.5f;
		case DIMENSION -> 7f;
		};
		var radians = Math.toRadians(-angleDegrees);
		// Centre the text vertically on its point: shift the baseline down along the text's own up.
		var drop = size * 0.35;
		var x = (float) (at.x() + Math.sin(radians) * drop);
		var y = (float) (y(at.y()) - Math.cos(radians) * drop);
		content.beginText();
		content.setFontAndSize(style == TextStyle.LABEL ? BOLD : REGULAR, size);
		content.setColorFill(style == TextStyle.LABEL ? new Color(0x21, 0x25, 0x29) : new Color(0x49, 0x50, 0x57));
		content.showTextAligned(Element.ALIGN_CENTER, text, x, y, (float) -angleDegrees);
		content.endText();
	}

	private void applyStroke(Stroke stroke) {
		switch (stroke) {
		case WALL -> {
			content.setLineWidth(1.6f);
			content.setColorStroke(new Color(0x21, 0x25, 0x29));
		}
		case OUTLINE -> {
			content.setLineWidth(0.9f);
			content.setColorStroke(new Color(0x21, 0x25, 0x29));
		}
		case THIN -> {
			content.setLineWidth(0.4f);
			content.setColorStroke(new Color(0x49, 0x50, 0x57));
		}
		case HIDDEN -> {
			content.setLineWidth(0.4f);
			content.setColorStroke(new Color(0x49, 0x50, 0x57));
			content.setLineDash(3f, 2f, 0f);
		}
		case NONE -> {
		}
		}
	}

	private static Color color(Fill fill) {
		return switch (fill) {
		case NONE, WHITE -> Color.WHITE;
		case LIGHT -> new Color(0xf1, 0xf3, 0xf5);
		case MEDIUM -> new Color(0xce, 0xd4, 0xda);
		case DARK -> new Color(0x49, 0x50, 0x57);
		case WALL -> new Color(0xad, 0xb5, 0xbd);
		case WINDOW -> new Color(0xdb, 0xe7, 0xfb);
		case APPLIANCE -> new Color(0xe9, 0xec, 0xef);
		};
	}

	private float y(double pageY) {
		return (float) (pageHeight - pageY);
	}

	private static BaseFont font(String name) {
		try {
			return BaseFont.createFont(name, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		} catch (Exception exception) {
			throw new IllegalStateException("could not load font " + name, exception);
		}
	}
}
