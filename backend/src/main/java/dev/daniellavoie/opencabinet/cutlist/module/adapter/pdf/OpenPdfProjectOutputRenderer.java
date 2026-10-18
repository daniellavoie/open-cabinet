package dev.daniellavoie.opencabinet.cutlist.module.adapter.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.cutlist.domain.model.PrintedOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.port.backend.ProjectOutputRenderer;
import dev.daniellavoie.opencabinet.drawing.domain.LengthFormat;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfViewPainter;

/**
 * Prints a Project's Cut List (with the Fronts to order), Nesting (a page per sheet, at a standard
 * scale) or Bill of Materials on landscape Letter pages, with the Shop Drawings' title block.
 */
@Component
public class OpenPdfProjectOutputRenderer implements ProjectOutputRenderer {
	static final float MARGIN = 36;
	static final float TITLE_BLOCK = 46;
	static final float GAP = 12;

	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);

	private final PdfViewPainter painter = new PdfViewPainter();

	@Override
	public byte[] pdf(PrintedOutput printed) {
		var output = new ByteArrayOutputStream();
		var page = PageSize.LETTER.rotate();
		var document = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN + TITLE_BLOCK + GAP);
		var writer = PdfWriter.getInstance(document, output);
		// The scale of the Nesting sheet on the page being drawn, for its title block.
		var scale = new AtomicReference<String>();
		writer.setPageEvent(new PdfPageEventHelper() {
			@Override
			public void onEndPage(PdfWriter pageWriter, Document pageDocument) {
				titleBlock(pageWriter, page, printed, pageWriter.getPageNumber(), scale.getAndSet(null));
			}
		});
		document.open();

		var language = printed.getLanguage();
		switch (printed.getDocument()) {
		case CUT_LIST -> cutList(document, printed, language);
		case NESTING -> nesting(document, writer, page, printed, language, scale);
		case BILL_OF_MATERIALS -> billOfMaterials(document, printed, language);
		}

		document.close();
		return output.toByteArray();
	}

	private void cutList(Document document, PrintedOutput printed, DrawingLanguage language) {
		var units = printed.getUnits();
		var rows = printed.getOutput().getCutList();
		if (rows.isEmpty()) {
			document.add(new Paragraph(language.pick("Aucune pièce : ajoutez des caissons au projet.",
					"No parts: add cabinets to the project."), body()));
		} else {
			var table = table(new float[] { 2.2f, 3f, 1.2f, 1.2f, 1f, 0.6f, 1.5f, 2.2f, 0.6f, 2f },
					language.pick("Pièce", "Part"), language.pick("Matériau", "Material"),
					language.pick("Longueur", "Length"), language.pick("Largeur", "Width"),
					language.pick("Épaisseur", "Thickness"), language.pick("Fil", "Grain"),
					language.pick("Chants (long. / larg.)", "Banded edges (len. / wid.)"),
					language.pick("Bande de chant", "Edge banding"), language.pick("Qté", "Qty"),
					language.pick("Caissons", "Cabinets"));
			for (var row : rows) {
				cells(table, language.partRole(row.getRole()), row.getMaterialName(),
						LengthFormat.format(row.getLengthMm(), units), LengthFormat.format(row.getWidthMm(), units),
						LengthFormat.format(row.getThicknessMm(), units), row.isGrain() ? language.pick("oui", "yes") : "",
						row.getBandedLengthEdges() + " / " + row.getBandedWidthEdges(),
						Optional.ofNullable(row.getEdgeBandingName()).orElse(""), Integer.toString(row.getQuantity()),
						String.join(", ", row.getCabinetLabels()));
			}
			document.add(table);
		}

		var ordered = Optional.ofNullable(printed.getOutput().getOrderedFronts()).orElse(List.of());
		if (!ordered.isEmpty()) {
			var heading = new Paragraph(language.pick("Façades à commander", "Fronts to order"), heading());
			heading.setSpacingBefore(18);
			heading.setSpacingAfter(6);
			document.add(heading);
			var table = table(new float[] { 2.2f, 3f, 1.2f, 1.2f, 0.6f, 2f }, language.pick("Pièce", "Part"),
					language.pick("Style", "Style"), language.pick("Hauteur", "Height"),
					language.pick("Largeur", "Width"), language.pick("Qté", "Qty"),
					language.pick("Caissons", "Cabinets"));
			for (var row : ordered) {
				cells(table, language.partRole(row.getRole()), row.getMaterialName(),
						LengthFormat.format(row.getHeightMm(), units), LengthFormat.format(row.getWidthMm(), units),
						Integer.toString(row.getQuantity()), String.join(", ", row.getCabinetLabels()));
			}
			document.add(table);
		}
	}

	/**
	 * A page per sheet, then the Parts too big for any sheet.
	 */
	private void nesting(Document document, PdfWriter writer, Rectangle page, PrintedOutput printed,
			DrawingLanguage language, AtomicReference<String> scale) {
		var sheets = printed.getSheets();
		if (sheets.isEmpty()) {
			document.add(new Paragraph(language.pick("Aucun panneau à découper.", "No sheets to cut."), body()));
		}
		for (int index = 0; index < sheets.size(); index++) {
			if (index > 0) {
				document.newPage();
			}
			scale.set(Optional.ofNullable(painter.drawAtScale(writer.getDirectContent(), List.of(sheets.get(index)),
					printed.getUnits(), MARGIN, MARGIN + TITLE_BLOCK + GAP, page.getWidth() - MARGIN,
					page.getHeight() - MARGIN - 4)).orElse(language.pick("hors échelle", "not to scale")));
			writer.setPageEmpty(false);
		}

		var oversized = printed.getOutput().getNesting().stream()
				.flatMap(material -> Optional.ofNullable(material.getOversizedParts()).orElse(List.of()).stream()
						.map(part -> material.getMaterialName() + " : " + part.getCabinetLabel() + " "
								+ language.partRole(part.getRole()) + " "
								+ LengthFormat.format(part.getLengthMm(), printed.getUnits()) + " × "
								+ LengthFormat.format(part.getWidthMm(), printed.getUnits())))
				.toList();
		if (!oversized.isEmpty()) {
			document.newPage();
			document.add(new Paragraph(language.pick("Pièces plus grandes que le panneau, à découper autrement",
					"Parts larger than the sheet, to cut another way"), heading()));
			oversized.forEach(line -> document.add(new Paragraph(line, body())));
		}
	}

	private void billOfMaterials(Document document, PrintedOutput printed, DrawingLanguage language) {
		var lines = printed.getOutput().getBillOfMaterials();
		if (lines.isEmpty()) {
			document.add(new Paragraph(language.pick("Rien à acheter : ajoutez des caissons au projet.",
					"Nothing to buy: add cabinets to the project."), body()));
			return;
		}

		var money = NumberFormat.getCurrencyInstance(language.locale());
		money.setCurrency(Currency.getInstance(printed.getCurrency()));
		var quantity = NumberFormat.getNumberInstance(language.locale());
		quantity.setMaximumFractionDigits(2);

		var table = table(new float[] { 1.6f, 4f, 1.4f, 1f, 1.2f, 1.2f }, language.pick("Type", "Kind"),
				language.pick("Article", "Item"), language.pick("Code", "Code"),
				language.pick("Quantité", "Quantity"), language.pick("Prix unitaire", "Unit price"),
				language.pick("Total", "Total"));
		for (var line : lines) {
			cells(table, language.materialKind(line.getKind()), line.getName(),
					Optional.ofNullable(line.getSku()).orElse(""), quantity.format(line.getQuantity()),
					money.format(line.getUnitPrice()), money.format(line.getTotal()));
		}
		var label = new PdfPCell(new Phrase(language.pick("Total des matériaux", "Materials total"), heading()));
		label.setColspan(5);
		label.setHorizontalAlignment(Element.ALIGN_RIGHT);
		label.setBorder(Rectangle.NO_BORDER);
		label.setPaddingTop(6);
		table.addCell(label);
		var total = new PdfPCell(new Phrase(money.format(Optional.ofNullable(printed.getOutput().getMaterialsTotal())
				.orElse(BigDecimal.ZERO)), heading()));
		total.setBorder(Rectangle.NO_BORDER);
		total.setPaddingTop(6);
		table.addCell(total);
		document.add(table);
	}

	private void titleBlock(PdfWriter writer, Rectangle page, PrintedOutput printed, int number, String scale) {
		var language = printed.getLanguage();
		var title = switch (printed.getDocument()) {
		case CUT_LIST -> language.pick("Liste de coupe", "Cut list");
		case NESTING -> language.pick("Optimisation des panneaux", "Nesting");
		case BILL_OF_MATERIALS -> language.pick("Liste des matériaux", "Bill of materials");
		};
		painter.titleBlock(writer.getDirectContent(), MARGIN, page.getWidth() - MARGIN, MARGIN + TITLE_BLOCK,
				printed.getShopName(),
				language.pick("Projet : ", "Project: ") + printed.getProjectName() + "   ·   "
						+ language.pick("Client : ", "Client: ") + printed.getClientName(),
				title,
				(scale == null ? "" : language.pick("Échelle ", "Scale ") + scale + "   ·   ")
						+ printed.getDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale()))
						+ "   ·   Page " + number);
	}

	private PdfPTable table(float[] widths, String... headers) {
		var table = new PdfPTable(widths);
		table.setWidthPercentage(100);
		table.setHeaderRows(1);
		for (var header : headers) {
			var cell = new PdfPCell(new Phrase(header, heading()));
			cell.setBorder(Rectangle.BOTTOM);
			cell.setBorderWidth(0.75f);
			cell.setPaddingBottom(4);
			table.addCell(cell);
		}
		return table;
	}

	private void cells(PdfPTable table, String... values) {
		for (var value : values) {
			var cell = new PdfPCell(new Phrase(value, body()));
			cell.setBorder(Rectangle.BOTTOM);
			cell.setBorderColor(RULE);
			cell.setBorderWidth(0.5f);
			cell.setPaddingTop(2);
			cell.setPaddingBottom(4);
			table.addCell(cell);
		}
	}

	private Font heading() {
		return new Font(painter.bold(), 8);
	}

	private Font body() {
		return new Font(painter.regular(), 8);
	}
}
