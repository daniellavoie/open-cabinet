package dev.daniellavoie.opencabinet.cutlist.module.adapter.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dev.daniellavoie.opencabinet.common.domain.Lengths;
import dev.daniellavoie.opencabinet.cutlist.domain.PartRoleNames;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.drawing.domain.NestingDrawings;
import dev.daniellavoie.opencabinet.drawing.module.adapter.pdf.PdfDrawingCanvas;

/**
 * Prints outputs for the shop floor: the Cut List and Bill of Materials as tables on Letter pages,
 * the Nesting one sheet per landscape page, each under a heading naming the Project.
 */
public final class OutputPdf {
	static final float MARGIN = 36;

	private static final Color MUTED = new Color(0x49, 0x50, 0x57);
	private static final Color RULE = new Color(0xde, 0xe2, 0xe6);
	private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD);
	private static final Font HEADING = new Font(Font.HELVETICA, 11, Font.BOLD);
	private static final Font HEAD = new Font(Font.HELVETICA, 8, Font.BOLD);
	private static final Font BODY = new Font(Font.HELVETICA, 8);
	private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);

	private OutputPdf() {
	}

	public static byte[] write(OutputDocument document, OutputKind kind) {
		var output = new ByteArrayOutputStream();
		var page = kind == OutputKind.NESTING ? PageSize.LETTER.rotate() : PageSize.LETTER;
		var pdf = new Document(page, MARGIN, MARGIN, MARGIN, MARGIN);
		var writer = PdfWriter.getInstance(pdf, output);
		pdf.open();

		heading(pdf, document, kind);
		switch (kind) {
		case CUT_LIST -> cutList(pdf, document);
		case BILL_OF_MATERIALS -> billOfMaterials(pdf, document);
		case NESTING -> nesting(pdf, writer, document);
		}

		pdf.close();
		return output.toByteArray();
	}

	private static void heading(Document pdf, OutputDocument document, OutputKind kind) {
		var language = document.language();
		pdf.add(new Paragraph(kind.title(language), TITLE));
		pdf.add(new Paragraph(document.shopName() + "  ·  " + document.projectName() + "  ·  " + document.clientName()
				+ "  ·  " + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", language.locale())), SMALL));
		pdf.add(new Paragraph(" ", SMALL));
	}

	private static void cutList(Document pdf, OutputDocument document) {
		var language = document.language();
		var units = document.units();
		var rows = document.output().getCutList();
		if (rows.isEmpty()) {
			pdf.add(new Paragraph(language.pick("Aucune pièce.", "No parts."), BODY));
		} else {
			var table = table(new float[] { 3.2f, 2.2f, 0.7f, 1.4f, 1.4f, 0.6f, 2.6f, 1.6f },
					language.pick("Matériau", "Material"), language.pick("Pièce", "Part"), language.pick("Qté", "Qty"),
					language.pick("Longueur", "Length"), language.pick("Largeur", "Width"), language.pick("Fil", "Grain"),
					language.pick("Chants (long. / larg.)", "Banding (length / width)"),
					language.pick("Caissons", "Cabinets"));
			for (var row : rows) {
				table.addCell(cell(row.getMaterialName(), Element.ALIGN_LEFT));
				table.addCell(cell(PartRoleNames.of(row.getRole(), language), Element.ALIGN_LEFT));
				table.addCell(cell(String.valueOf(row.getQuantity()), Element.ALIGN_RIGHT));
				table.addCell(cell(Lengths.format(row.getLengthMm(), units), Element.ALIGN_RIGHT));
				table.addCell(cell(Lengths.format(row.getWidthMm(), units), Element.ALIGN_RIGHT));
				table.addCell(cell(row.isGrain() ? "↕" : "", Element.ALIGN_CENTER));
				table.addCell(cell(row.getEdgeBandingId() == null ? "—"
						: row.getBandedLengthEdges() + " / " + row.getBandedWidthEdges() + "  "
								+ Optional.ofNullable(row.getEdgeBandingName()).orElse(""), Element.ALIGN_LEFT));
				table.addCell(cell(String.join(", ", row.getCabinetLabels()), Element.ALIGN_LEFT));
			}
			pdf.add(table);
		}

		var ordered = Optional.ofNullable(document.output().getOrderedFronts()).orElse(List.of());
		if (!ordered.isEmpty()) {
			pdf.add(new Paragraph(" ", SMALL));
			pdf.add(new Paragraph(language.pick("Façades à commander", "Fronts to order"), HEADING));
			pdf.add(new Paragraph(" ", SMALL));
			var table = table(new float[] { 3.6f, 2.2f, 0.7f, 1.4f, 1.4f, 1.6f }, language.pick("Style", "Style"),
					language.pick("Pièce", "Part"), language.pick("Qté", "Qty"), language.pick("Hauteur", "Height"),
					language.pick("Largeur", "Width"), language.pick("Caissons", "Cabinets"));
			for (var row : ordered) {
				table.addCell(cell(row.getMaterialName(), Element.ALIGN_LEFT));
				table.addCell(cell(PartRoleNames.of(row.getRole(), language), Element.ALIGN_LEFT));
				table.addCell(cell(String.valueOf(row.getQuantity()), Element.ALIGN_RIGHT));
				table.addCell(cell(Lengths.format(row.getHeightMm(), units), Element.ALIGN_RIGHT));
				table.addCell(cell(Lengths.format(row.getWidthMm(), units), Element.ALIGN_RIGHT));
				table.addCell(cell(String.join(", ", row.getCabinetLabels()), Element.ALIGN_LEFT));
			}
			pdf.add(table);
		}
	}

	private static void billOfMaterials(Document pdf, OutputDocument document) {
		var language = document.language();
		var money = NumberFormat.getCurrencyInstance(language.locale());
		money.setCurrency(Currency.getInstance(document.currency()));
		var quantity = NumberFormat.getNumberInstance(language.locale());
		quantity.setMaximumFractionDigits(2);

		var table = table(new float[] { 1.6f, 4f, 1.4f, 1.6f, 1.4f, 1.4f }, language.pick("Type", "Kind"),
				language.pick("Article", "Item"), language.pick("Code", "SKU"), language.pick("Quantité", "Quantity"),
				language.pick("Prix unitaire", "Unit price"), language.pick("Total", "Total"));
		for (var line : document.output().getBillOfMaterials()) {
			table.addCell(cell(OutputCsv.kind(line.getKind(), language), Element.ALIGN_LEFT));
			table.addCell(cell(line.getName(), Element.ALIGN_LEFT));
			table.addCell(cell(Optional.ofNullable(line.getSku()).orElse(""), Element.ALIGN_LEFT));
			table.addCell(cell(quantity.format(line.getQuantity()) + " " + OutputCsv.unit(line.getKind(), language),
					Element.ALIGN_RIGHT));
			table.addCell(cell(money.format(line.getUnitPrice()), Element.ALIGN_RIGHT));
			table.addCell(cell(money.format(line.getTotal()), Element.ALIGN_RIGHT));
		}
		var total = new PdfPCell(new Phrase(language.pick("Total des matériaux", "Materials total"), HEAD));
		total.setColspan(5);
		total.setHorizontalAlignment(Element.ALIGN_RIGHT);
		total.setBorder(Rectangle.NO_BORDER);
		total.setPadding(4);
		table.addCell(total);
		var amount = new PdfPCell(new Phrase(money.format(document.output().getMaterialsTotal()), HEAD));
		amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
		amount.setBorder(Rectangle.NO_BORDER);
		amount.setPadding(4);
		table.addCell(amount);
		pdf.add(table);
	}

	private static void nesting(Document pdf, PdfWriter writer, OutputDocument document) {
		var language = document.language();
		var page = pdf.getPageSize();
		var first = true;
		for (var nesting : document.output().getNesting()) {
			for (int sheet = 0; sheet < nesting.getSheets().size(); sheet++) {
				if (!first) {
					pdf.newPage();
				}
				var top = first ? MARGIN + 50 : MARGIN;
				first = false;
				var drawing = NestingDrawings.sheet(nesting, sheet, document.units(), language);
				pdf.add(new Paragraph(drawing.title(), HEADING));
				PdfDrawingCanvas.paint(drawing, writer.getDirectContent(), page.getHeight(), MARGIN, top + 20,
						page.getWidth() - 2 * MARGIN, page.getHeight() - top - 20 - MARGIN);
			}
			if (!nesting.getOversizedParts().isEmpty()) {
				pdf.add(new Paragraph(nesting.getMaterialName() + " : " + nesting.getOversizedParts().size() + " "
						+ language.pick("pièce(s) plus grande(s) que le panneau.", "part(s) larger than the sheet."), SMALL));
			}
		}
		if (first) {
			pdf.add(new Paragraph(language.pick("Aucun panneau à découper.", "No sheets to cut."), BODY));
		}
	}

	private static PdfPTable table(float[] widths, String... headers) {
		var table = new PdfPTable(widths);
		table.setWidthPercentage(100);
		table.setHeaderRows(1);
		for (var header : headers) {
			var cell = new PdfPCell(new Phrase(header, HEAD));
			cell.setBorder(Rectangle.BOTTOM);
			cell.setBorderColor(MUTED);
			cell.setPadding(4);
			table.addCell(cell);
		}
		return table;
	}

	private static PdfPCell cell(String text, int alignment) {
		var cell = new PdfPCell(new Phrase(text, BODY));
		cell.setHorizontalAlignment(alignment);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(RULE);
		cell.setPadding(4);
		return cell;
	}
}
