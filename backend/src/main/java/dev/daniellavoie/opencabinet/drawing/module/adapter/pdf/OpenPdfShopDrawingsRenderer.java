package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.drawing.domain.model.PartsTable;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Line;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Point;
import dev.daniellavoie.opencabinet.drawing.domain.model.Sheet;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.drawing.domain.port.backend.ShopDrawingsRenderer;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Prints each Sheet on a landscape Letter page: its Views side by side at the largest standard
 * scale that fits (architectural for imperial Shops, 1:N for metric ones), its notes, its Parts
 * table, and a title block.
 */
@Component
public class OpenPdfShopDrawingsRenderer implements ShopDrawingsRenderer {
	static final float MARGIN = 36;
	static final float TITLE_BLOCK = 46;
	static final float GAP = 18;
	/**
	 * Distance between dimension lines, and from the first one to what it measures.
	 */
	static final float DIMENSION_STEP = 14;
	static final float TEXT_SIZE = 7;
	static final float POINTS_PER_MM = 72 / 25.4f;

	/**
	 * Standard scales, largest first: {@code ratio} is model length over paper length.
	 */
	record Scale(double ratio, String imperial, String metric) {
	}

	static final List<Scale> IMPERIAL_SCALES = List.of(
			new Scale(4, "3\" = 1'-0\"", null),
			new Scale(8, "1 1/2\" = 1'-0\"", null),
			new Scale(12, "1\" = 1'-0\"", null),
			new Scale(16, "3/4\" = 1'-0\"", null),
			new Scale(24, "1/2\" = 1'-0\"", null),
			new Scale(32, "3/8\" = 1'-0\"", null),
			new Scale(48, "1/4\" = 1'-0\"", null),
			new Scale(64, "3/16\" = 1'-0\"", null),
			new Scale(96, "1/8\" = 1'-0\"", null));
	static final List<Scale> METRIC_SCALES = List.of(
			new Scale(5, null, "1:5"),
			new Scale(10, null, "1:10"),
			new Scale(15, null, "1:15"),
			new Scale(20, null, "1:20"),
			new Scale(25, null, "1:25"),
			new Scale(30, null, "1:30"),
			new Scale(40, null, "1:40"),
			new Scale(50, null, "1:50"),
			new Scale(75, null, "1:75"),
			new Scale(100, null, "1:100"));

	private static final Color INK = Color.BLACK;
	private static final Color MUTED = new Color(0x49, 0x50, 0x57);
	private static final Color WALL_FILL = new Color(0xce, 0xd4, 0xda);
	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);

	private final BaseFont regular;
	private final BaseFont bold;

	public OpenPdfShopDrawingsRenderer() {
		try {
			regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		} catch (Exception exception) {
			throw new IllegalStateException("could not load the PDF fonts", exception);
		}
	}

	@Override
	public byte[] pdf(ShopDrawings drawings) {
		var output = new ByteArrayOutputStream();
		var page = PageSize.LETTER.rotate();
		var document = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN);
		var writer = PdfWriter.getInstance(document, output);
		document.open();

		var sheets = drawings.getSheets();
		for (int index = 0; index < sheets.size(); index++) {
			if (index > 0) {
				document.newPage();
			}
			sheet(writer.getDirectContent(), page, drawings, sheets.get(index), index + 1, sheets.size());
		}

		document.close();
		return output.toByteArray();
	}

	private void sheet(PdfContentByte canvas, Rectangle page, ShopDrawings drawings, Sheet sheet, int number,
			int count) {
		var left = MARGIN;
		var right = page.getWidth() - MARGIN;
		var top = page.getHeight() - MARGIN;
		var bottom = MARGIN + TITLE_BLOCK + GAP / 2;

		var table = Optional.ofNullable(sheet.getParts()).map(parts -> table(parts, right - left)).orElse(null);
		if (table != null) {
			var tableTop = bottom + table.getTotalHeight();
			table.writeSelectedRows(0, -1, left, tableTop, canvas);
			bottom = tableTop + GAP / 2;
		}

		var notes = Optional.ofNullable(sheet.getNotes()).orElse(List.of());
		for (int index = notes.size() - 1; index >= 0; index--) {
			text(canvas, regular, 8, MUTED, notes.get(index), left, bottom, 0, Element.ALIGN_LEFT);
			bottom += 11;
		}
		bottom += GAP / 2;

		var scale = views(canvas, sheet.getViews(), drawings.getUnits(), left, bottom, right, top);
		titleBlock(canvas, drawings, sheet, scale, number, count, left, right);
	}

	/**
	 * Draws the Views side by side, centred, at one scale; returns the scale's name.
	 */
	private String views(PdfContentByte canvas, List<View> views, Units units, float left, float bottom, float right,
			float top) {
		var boxes = views.stream().map(ViewBox::of).toList();
		var titled = views.stream().anyMatch(view -> view.getTitle() != null && !view.getTitle().isBlank());
		var availableWidth = right - left - GAP * (views.size() - 1);
		var availableHeight = top - bottom - (titled ? 14 : 0);

		var scales = units == Units.METRIC ? METRIC_SCALES : IMPERIAL_SCALES;
		var chosen = scales.stream()
				.filter(scale -> fits(boxes, POINTS_PER_MM / scale.ratio(), availableWidth, availableHeight))
				.findFirst();
		double pointsPerMm = chosen.map(scale -> POINTS_PER_MM / scale.ratio())
				.orElseGet(() -> fit(boxes, availableWidth, availableHeight));

		var totalWidth = boxes.stream().mapToDouble(box -> box.width(pointsPerMm)).sum() + GAP * (views.size() - 1);
		var x = left + (right - left - totalWidth) / 2;
		for (int index = 0; index < views.size(); index++) {
			var box = boxes.get(index);
			var height = box.height(pointsPerMm);
			var y = bottom + (availableHeight - height) / 2;
			var view = views.get(index);
			if (view.getTitle() != null && !view.getTitle().isBlank()) {
				text(canvas, bold, 8, INK, view.getTitle(), (float) (x + box.width(pointsPerMm) / 2),
						(float) (bottom + availableHeight + 4), 0, Element.ALIGN_CENTER);
			}
			draw(canvas, view, box, pointsPerMm, (float) x, (float) y);
			x += box.width(pointsPerMm) + GAP;
		}

		return chosen.map(scale -> units == Units.METRIC ? scale.metric() : scale.imperial()).orElse(null);
	}

	private static boolean fits(List<ViewBox> boxes, double pointsPerMm, double width, double height) {
		return boxes.stream().mapToDouble(box -> box.width(pointsPerMm)).sum() <= width
				&& boxes.stream().allMatch(box -> box.height(pointsPerMm) <= height);
	}

	/**
	 * The scale that just fits, for Rooms too big for every standard scale.
	 */
	private static double fit(List<ViewBox> boxes, double width, double height) {
		var paddingWidth = boxes.stream().mapToDouble(box -> 2 * box.padding()).sum();
		var modelWidth = boxes.stream().mapToDouble(ViewBox::modelWidth).sum();
		var widest = boxes.stream().mapToDouble(box -> (height - 2 * box.padding()) / Math.max(box.modelHeight(), 1))
				.min().orElse(1);
		return Math.max(0.001, Math.min((width - paddingWidth) / Math.max(modelWidth, 1), widest));
	}

	/**
	 * A View's extent in millimetres, and the room its dimension lines need around it on paper.
	 */
	record ViewBox(double minX, double minY, double modelWidth, double modelHeight, double padding) {
		static ViewBox of(View view) {
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

		double width(double pointsPerMm) {
			return modelWidth * pointsPerMm + 2 * padding;
		}

		double height(double pointsPerMm) {
			return modelHeight * pointsPerMm + 2 * padding;
		}
	}

	private void draw(PdfContentByte canvas, View view, ViewBox box, double pointsPerMm, float x, float y) {
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
				case WALL, OPENING -> canvas.fillStroke();
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

	private void titleBlock(PdfContentByte canvas, ShopDrawings drawings, Sheet sheet, String scale, int number,
			int count, float left, float right) {
		var language = drawings.getLanguage();
		var top = MARGIN + TITLE_BLOCK;
		canvas.saveState();
		canvas.setLineWidth(0.75f);
		canvas.setColorStroke(INK);
		canvas.moveTo(left, top);
		canvas.lineTo(right, top);
		canvas.stroke();
		canvas.restoreState();

		text(canvas, bold, 11, INK, drawings.getShopName(), left, top - 16, 0, Element.ALIGN_LEFT);
		text(canvas, regular, 8, MUTED, language.pick("Projet : ", "Project: ") + drawings.getProjectName()
				+ "   ·   " + language.pick("Client : ", "Client: ") + drawings.getClientName(), left, top - 30, 0,
				Element.ALIGN_LEFT);
		text(canvas, bold, 11, INK, sheet.getTitle(), right, top - 16, 0, Element.ALIGN_RIGHT);
		text(canvas, regular, 8, MUTED, String.join("   ·   ",
				language.pick("Échelle ", "Scale ") + Optional.ofNullable(scale)
						.orElse(language.pick("hors échelle", "not to scale")),
				drawings.getDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale())),
				language.pick("Feuille ", "Sheet ") + number + " / " + count), right, top - 30, 0,
				Element.ALIGN_RIGHT);
	}

	private PdfPTable table(PartsTable parts, float width) {
		var table = new PdfPTable(new float[] { 2.2f, 3.2f, 1.1f, 1.1f, 1.1f, 1.6f, 0.6f });
		table.setTotalWidth(width);
		table.setLockedWidth(true);
		var header = new Font(bold, 7.5f);
		var body = new Font(regular, 7.5f);
		parts.getHeaders().forEach(text -> table.addCell(cell(text, header, true)));
		parts.getRows().forEach(row -> row.forEach(text -> table.addCell(cell(text, body, false))));
		return table;
	}

	private static PdfPCell cell(String text, Font font, boolean header) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(header ? INK : RULE);
		cell.setBorderWidth(header ? 0.75f : 0.5f);
		cell.setPaddingTop(2);
		cell.setPaddingBottom(3);
		return cell;
	}

	private static void text(PdfContentByte canvas, BaseFont font, float size, Color color, String text, float x,
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
