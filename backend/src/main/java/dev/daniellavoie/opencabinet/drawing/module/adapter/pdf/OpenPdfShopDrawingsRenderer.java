package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.INK;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.MUTED;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.POINTS_PER_MM;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.fit;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.fits;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.text;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.drawing.domain.model.PartsTable;
import dev.daniellavoie.opencabinet.drawing.domain.model.Sheet;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;
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

	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);

	private final PdfViewPainter painter = new PdfViewPainter();

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
			text(canvas, painter.regular(), 8, MUTED, notes.get(index), left, bottom, 0, Element.ALIGN_LEFT);
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
		var boxes = views.stream().map(PdfViewPainter.ViewBox::of).toList();
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
				text(canvas, painter.bold(), 8, INK, view.getTitle(), (float) (x + box.width(pointsPerMm) / 2),
						(float) (bottom + availableHeight + 4), 0, Element.ALIGN_CENTER);
			}
			painter.draw(canvas, view, box, pointsPerMm, (float) x, (float) y);
			x += box.width(pointsPerMm) + GAP;
		}

		return chosen.map(scale -> units == Units.METRIC ? scale.metric() : scale.imperial()).orElse(null);
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

		text(canvas, painter.bold(), 11, INK, drawings.getShopName(), left, top - 16, 0, Element.ALIGN_LEFT);
		text(canvas, painter.regular(), 8, MUTED, language.pick("Projet : ", "Project: ") + drawings.getProjectName()
				+ "   ·   " + language.pick("Client : ", "Client: ") + drawings.getClientName(), left, top - 30, 0,
				Element.ALIGN_LEFT);
		text(canvas, painter.bold(), 11, INK, sheet.getTitle(), right, top - 16, 0, Element.ALIGN_RIGHT);
		text(canvas, painter.regular(), 8, MUTED, String.join("   ·   ",
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
		var header = new Font(painter.bold(), 7.5f);
		var body = new Font(painter.regular(), 7.5f);
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

}
