package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.INK;
import static dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter.MUTED;
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
import dev.daniellavoie.opencabinet.drawing.domain.port.backend.ShopDrawingsRenderer;

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

		var scale = painter.drawAtScale(canvas, sheet.getViews(), drawings.getUnits(), left, bottom, right, top);
		titleBlock(canvas, drawings, sheet, scale, number, count, left, right);
	}

	private void titleBlock(PdfContentByte canvas, ShopDrawings drawings, Sheet sheet, String scale, int number,
			int count, float left, float right) {
		var language = drawings.getLanguage();
		painter.titleBlock(canvas, left, right, MARGIN + TITLE_BLOCK, drawings.getShopName(),
				language.pick("Projet : ", "Project: ") + drawings.getProjectName() + "   ·   "
						+ language.pick("Client : ", "Client: ") + drawings.getClientName(),
				sheet.getTitle(),
				String.join("   ·   ",
						language.pick("Échelle ", "Scale ") + Optional.ofNullable(scale)
								.orElse(language.pick("hors échelle", "not to scale")),
						drawings.getDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale())),
						language.pick("Feuille ", "Sheet ") + number + " / " + count));
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
