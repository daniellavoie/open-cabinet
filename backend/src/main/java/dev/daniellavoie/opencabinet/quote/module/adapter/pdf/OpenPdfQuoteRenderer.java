package dev.daniellavoie.opencabinet.quote.module.adapter.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter;
import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuotePictures;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import dev.daniellavoie.opencabinet.quote.domain.port.backend.QuoteRenderer;

/**
 * Renders the Client-facing Quote in French (Canada): the Room in 3D under the parties, the price,
 * then an Elevation of each Wall with Cabinets. The internal breakdown is never printed.
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
	private static final float OVERVIEW_HEIGHT = 240;
	private static final float ELEVATION_HEIGHT = 250;

	private final PdfViewPainter painter = new PdfViewPainter();

	@Override
	public byte[] pdf(Quote quote) {
		var content = quote.getContent();
		var money = NumberFormat.getCurrencyInstance(LOCALE);
		money.setCurrency(Currency.getInstance(content.getCurrency()));

		var output = new ByteArrayOutputStream();
		var document = new Document(PageSize.LETTER, 54, 54, 54, 54);
		var writer = PdfWriter.getInstance(document, output);
		document.open();
		var width = document.right() - document.left();
		var pictures = Optional.ofNullable(quote.getPictures()).map(QuotePictures::getViews).orElse(List.of());

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

		if (!pictures.isEmpty()) {
			document.add(picture(writer, pictures.get(0), width, OVERVIEW_HEIGHT));
			document.add(spacer());
		}

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

		for (var elevation : pictures.stream().skip(1).toList()) {
			document.add(titled(elevation.getTitle(), picture(writer, elevation, width, ELEVATION_HEIGHT)));
		}

		document.close();
		return output.toByteArray();
	}

	/**
	 * A View drawn as large as fits {@code width × height}, centred.
	 */
	private Image picture(PdfWriter writer, View view, float width, float height) {
		var box = PdfViewPainter.ViewBox.of(view);
		var pointsPerMm = PdfViewPainter.fit(List.of(box), width, height);
		var template = writer.getDirectContent().createTemplate((float) box.width(pointsPerMm),
				(float) box.height(pointsPerMm));
		painter.draw(template, view, box, pointsPerMm, 0, 0);
		try {
			var image = Image.getInstance(template);
			image.setAlignment(Element.ALIGN_CENTER);
			return image;
		} catch (BadElementException exception) {
			throw new IllegalStateException("could not place a picture in the quote", exception);
		}
	}

	/**
	 * A heading and its picture, moved to the next page together when they don't fit.
	 */
	private static PdfPTable titled(String title, Image picture) {
		var table = new PdfPTable(1);
		table.setWidthPercentage(100);
		table.setKeepTogether(true);
		table.setSpacingBefore(18);
		var heading = new PdfPCell(new Phrase(title, HEADING));
		heading.setBorder(Rectangle.NO_BORDER);
		heading.setPaddingBottom(6);
		table.addCell(heading);
		var cell = new PdfPCell(picture, false);
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
		return table;
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
