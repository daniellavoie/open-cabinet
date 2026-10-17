package dev.daniellavoie.opencabinet.quote.module.adapter.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfDrawingCanvas;
import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuotePictures;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import dev.daniellavoie.opencabinet.quote.domain.port.backend.QuoteRenderer;

/**
 * Renders the Client-facing Quote in French (Canada): the price on the first page, then pictures of
 * the design (a 3D view and the plan, then the Elevations two to a page). The internal breakdown is
 * never printed.
 */
@Component
public class OpenPdfQuoteRenderer implements QuoteRenderer {
	private static final Locale LOCALE = Locale.CANADA_FRENCH;
	private static final Color MUTED = new Color(0x49, 0x50, 0x57);
	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);

	private static final Font TITLE = new Font(Font.HELVETICA, 20, Font.BOLD);
	private static final Font HEADING = new Font(Font.HELVETICA, 11, Font.BOLD);
	private static final Font BODY = new Font(Font.HELVETICA, 10);
	private static final Font SMALL = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);
	private static final Font TOTAL = new Font(Font.HELVETICA, 12, Font.BOLD);

	static final float PICTURE_GAP = 18;

	@Override
	public byte[] pdf(Quote quote, QuotePictures pictures) {
		var content = quote.getContent();
		var money = NumberFormat.getCurrencyInstance(LOCALE);
		money.setCurrency(Currency.getInstance(content.getCurrency()));

		var output = new ByteArrayOutputStream();
		var document = new Document(PageSize.LETTER, 54, 54, 54, 54);
		var writer = PdfWriter.getInstance(document, output);
		document.open();

		document.add(new Paragraph(content.getShopName(), TITLE));
		document.add(new Paragraph("Soumission " + quote.getDisplayNumber() + " — "
				+ quote.getCreationTimestamp().format(DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE)), SMALL));
		if (quote.getRevision() > 0) {
			document.add(new Paragraph("Révision " + quote.getRevision()
					+ " : remplace les versions précédentes de cette soumission.", SMALL));
		}
		document.add(spacer());

		var parties = new PdfPTable(2);
		parties.setWidthPercentage(100);
		parties.addCell(block("Client", Stream.of(content.getClientName(), content.getClientAddress(),
				content.getClientEmail(), content.getClientPhone())));
		parties.addCell(block("Projet", Stream.of(content.getProjectName(),
				quote.getStatus() == QuoteStatus.DRAFT ? "Brouillon" : null)));
		document.add(parties);
		document.add(spacer());

		var lines = new PdfPTable(new float[] { 4, 1 });
		lines.setWidthPercentage(100);
		lines.addCell(headerCell("Description", Element.ALIGN_LEFT));
		lines.addCell(headerCell("Montant", Element.ALIGN_RIGHT));
		content.getLines().forEach(line -> {
			lines.addCell(cell(line.getDescription(), BODY, Element.ALIGN_LEFT, true));
			lines.addCell(cell(money.format(line.getAmount()), BODY, Element.ALIGN_RIGHT, true));
		});
		lines.addCell(cell("Sous-total", HEADING, Element.ALIGN_RIGHT, false));
		lines.addCell(cell(money.format(content.getSubtotal()), HEADING, Element.ALIGN_RIGHT, false));
		content.getTaxes().forEach(tax -> {
			lines.addCell(cell(tax.getName() + " (" + percent(tax.getRatePercent()) + ")", BODY, Element.ALIGN_RIGHT,
					false));
			lines.addCell(cell(money.format(tax.getAmount()), BODY, Element.ALIGN_RIGHT, false));
		});
		lines.addCell(cell("Total", TOTAL, Element.ALIGN_RIGHT, false));
		lines.addCell(cell(money.format(content.getTotal()), TOTAL, Element.ALIGN_RIGHT, false));
		document.add(lines);

		document.add(spacer());
		document.add(new Paragraph("Les prix sont en " + content.getCurrency()
				+ ". Cette soumission correspond à la conception du projet à la date ci-dessus.", SMALL));

		if (!pictures.isEmpty()) {
			pictures(document, writer, quote, pictures);
		}

		document.close();
		return output.toByteArray();
	}

	/**
	 * The 3D view above the plan, then two Elevations per page, each under its caption.
	 */
	private static void pictures(Document document, PdfWriter writer, Quote quote, QuotePictures pictures) {
		var page = document.getPageSize();
		var left = document.leftMargin();
		var width = page.getWidth() - left - document.rightMargin();
		var top = document.topMargin() + 24;
		var half = (page.getHeight() - top - document.bottomMargin() - PICTURE_GAP) / 2;
		var heading = "Soumission " + quote.getDisplayNumber() + " — " + quote.getContent().getProjectName();

		var drawings = new java.util.ArrayList<Drawing>();
		drawings.add(pictures.view());
		drawings.add(pictures.plan());
		drawings.addAll(pictures.elevations());
		for (int index = 0; index < drawings.size(); index++) {
			if (index % 2 == 0) {
				document.newPage();
				ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, new Phrase(heading, SMALL),
						left, page.getHeight() - document.topMargin(), 0);
			}
			var drawing = drawings.get(index);
			var boxTop = top + (index % 2) * (half + PICTURE_GAP);
			ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
					new Phrase(drawing.title(), HEADING), left, page.getHeight() - boxTop - 4, 0);
			PdfDrawingCanvas.paint(drawing, writer.getDirectContent(), page.getHeight(), left, boxTop + 10, width,
					half - 10);
		}
	}

	private static String percent(BigDecimal rate) {
		var format = NumberFormat.getNumberInstance(LOCALE);
		format.setMaximumFractionDigits(3);
		return format.format(rate) + " %";
	}

	private static PdfPCell block(String title, Stream<String> lines) {
		var phrase = new Phrase();
		phrase.add(new Phrase(title + "\n", HEADING));
		lines.map(Optional::ofNullable)
				.flatMap(Optional::stream)
				.filter(line -> !line.isBlank())
				.forEach(line -> phrase.add(new Phrase(line + "\n", BODY)));

		var cell = new PdfPCell(phrase);
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPaddingBottom(6);
		return cell;
	}

	private static PdfPCell headerCell(String text, int alignment) {
		var cell = cell(text, HEADING, alignment, true);
		cell.setBorderWidthBottom(1);
		return cell;
	}

	private static PdfPCell cell(String text, Font font, int alignment, boolean ruled) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setHorizontalAlignment(alignment);
		cell.setBorder(ruled ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
		cell.setBorderColor(RULE);
		cell.setPadding(6);
		return cell;
	}

	private static Paragraph spacer() {
		return new Paragraph(" ", BODY);
	}
}
