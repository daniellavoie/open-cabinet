package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.common.domain.Lengths;
import dev.daniellavoie.opencabinet.cutlist.domain.PartRoleNames;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSet;
import dev.daniellavoie.opencabinet.drawing.domain.port.backend.ShopDrawingsRenderer;

/**
 * Prints the Shop Drawings one per landscape Letter page, each with a title block: Shop, Project,
 * Client, drawing title, date and page. Cabinet details carry the Cabinet's Parts beside the
 * drawing.
 */
@Component
public class OpenPdfShopDrawingsRenderer implements ShopDrawingsRenderer {
	static final float MARGIN = 36;
	static final float TITLE_BLOCK = 40;
	static final float PARTS_WIDTH = 250;

	private static final Color MUTED = new Color(0x49, 0x50, 0x57);
	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);
	private static final Font TITLE = new Font(Font.HELVETICA, 12, Font.BOLD);
	private static final Font BODY = new Font(Font.HELVETICA, 8);
	private static final Font HEAD = new Font(Font.HELVETICA, 8, Font.BOLD);
	private static final Font SMALL = new Font(Font.HELVETICA, 7, Font.NORMAL, MUTED);

	@Override
	public byte[] pdf(ShopDrawingSet set) {
		var language = set.language();
		var page = PageSize.LETTER.rotate();
		var output = new ByteArrayOutputStream();
		var document = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN);
		var writer = PdfWriter.getInstance(document, output);
		document.open();

		var date = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale()));
		var total = set.sheets().size();
		for (int index = 0; index < total; index++) {
			var sheet = set.sheets().get(index);
			if (index > 0) {
				document.newPage();
			}
			var content = writer.getDirectContent();
			var width = page.getWidth() - 2 * MARGIN;
			var height = page.getHeight() - 2 * MARGIN - TITLE_BLOCK;

			ColumnText.showTextAligned(content, Element.ALIGN_LEFT, new Phrase(sheet.drawing().title(), TITLE), MARGIN,
					page.getHeight() - MARGIN - 12, 0);
			ColumnText.showTextAligned(content, Element.ALIGN_LEFT,
					new Phrase(set.shopName() + "  ·  " + set.projectName() + "  ·  " + set.clientName(), SMALL), MARGIN,
					page.getHeight() - MARGIN - 26, 0);
			ColumnText.showTextAligned(content, Element.ALIGN_RIGHT, new Phrase(date + "  ·  "
					+ language.pick("Page ", "Page ") + (index + 1) + " / " + total, SMALL),
					page.getWidth() - MARGIN, page.getHeight() - MARGIN - 26, 0);
			content.saveState();
			content.setColorStroke(RULE);
			content.setLineWidth(0.5f);
			content.moveTo(MARGIN, page.getHeight() - MARGIN - TITLE_BLOCK + 6);
			content.lineTo(page.getWidth() - MARGIN, page.getHeight() - MARGIN - TITLE_BLOCK + 6);
			content.stroke();
			content.restoreState();

			var drawingWidth = sheet.parts().isEmpty() ? width : width - PARTS_WIDTH - 12;
			PdfDrawingCanvas.paint(sheet.drawing(), content, page.getHeight(), MARGIN, MARGIN + TITLE_BLOCK,
					drawingWidth, height);

			if (!sheet.parts().isEmpty()) {
				var table = new PdfPTable(new float[] { 3.2f, 0.8f, 3.2f });
				table.setTotalWidth(PARTS_WIDTH);
				table.addCell(cell(language.pick("Pièce", "Part"), HEAD, Element.ALIGN_LEFT));
				table.addCell(cell(language.pick("Qté", "Qty"), HEAD, Element.ALIGN_RIGHT));
				table.addCell(cell(language.pick("Longueur × largeur", "Length × width"), HEAD, Element.ALIGN_RIGHT));
				for (var row : sheet.parts()) {
					table.addCell(cell(PartRoleNames.of(row.getRole(), language) + "\n" + row.getMaterialName(), BODY,
							Element.ALIGN_LEFT));
					table.addCell(cell(String.valueOf(row.getQuantity()), BODY, Element.ALIGN_RIGHT));
					table.addCell(cell(Lengths.format(row.getLengthMm(), set.units()) + " × "
							+ Lengths.format(row.getWidthMm(), set.units()), BODY, Element.ALIGN_RIGHT));
				}
				table.writeSelectedRows(0, -1, page.getWidth() - MARGIN - PARTS_WIDTH,
						page.getHeight() - MARGIN - TITLE_BLOCK, content);
			}
		}

		document.close();
		return output.toByteArray();
	}

	private static PdfPCell cell(String text, Font font, int alignment) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setHorizontalAlignment(alignment);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(RULE);
		cell.setPadding(3);
		return cell;
	}
}
