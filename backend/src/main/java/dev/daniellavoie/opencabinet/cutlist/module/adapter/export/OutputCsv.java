package dev.daniellavoie.opencabinet.cutlist.module.adapter.export;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.common.domain.Lengths;
import dev.daniellavoie.opencabinet.cutlist.domain.PartRoleNames;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Writes outputs as CSV for spreadsheets: comma separated, UTF-8 with a byte order mark so
 * spreadsheet programs read accents right, headers in the reader's language. Lengths come in
 * millimetres and, for imperial Shops, also as the Shop writes them.
 */
public final class OutputCsv {
	private static final String BOM = "\uFEFF";
	private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

	private OutputCsv() {
	}

	public static byte[] write(OutputDocument document, OutputKind kind) {
		var rows = switch (kind) {
		case CUT_LIST -> cutList(document);
		case NESTING -> nesting(document);
		case BILL_OF_MATERIALS -> billOfMaterials(document);
		};
		return (BOM + rows.stream().map(row -> row.stream().map(OutputCsv::field).collect(Collectors.joining(",")))
				.collect(Collectors.joining("\r\n", "", "\r\n"))).getBytes(StandardCharsets.UTF_8);
	}

	private static List<List<String>> cutList(OutputDocument document) {
		var language = document.language();
		var rows = new ArrayList<List<String>>();
		rows.add(Stream.concat(Stream.of(
				language.pick("Matériau", "Material"),
				language.pick("Pièce", "Part"),
				language.pick("Qté", "Qty"),
				language.pick("Longueur (mm)", "Length (mm)"),
				language.pick("Largeur (mm)", "Width (mm)"),
				language.pick("Épaisseur (mm)", "Thickness (mm)"),
				language.pick("Fil", "Grain"),
				language.pick("Chants longueur", "Banded length edges"),
				language.pick("Chants largeur", "Banded width edges"),
				language.pick("Bande de chant", "Edge banding"),
				language.pick("Commandée", "Ordered"),
				language.pick("Caissons", "Cabinets")), imperialHeaders(document)).toList());
		for (var row : document.output().getCutList()) {
			rows.add(Stream.concat(Stream.of(
					row.getMaterialName(),
					PartRoleNames.of(row.getRole(), language),
					String.valueOf(row.getQuantity()),
					number(row.getLengthMm()),
					number(row.getWidthMm()),
					number(row.getThicknessMm()),
					yes(row.isGrain(), language),
					String.valueOf(row.getBandedLengthEdges()),
					String.valueOf(row.getBandedWidthEdges()),
					Optional.ofNullable(row.getEdgeBandingName()).orElse(""),
					yes(false, language),
					String.join(" ", row.getCabinetLabels())), imperial(document, row.getLengthMm(), row.getWidthMm()))
					.toList());
		}
		for (var row : Optional.ofNullable(document.output().getOrderedFronts()).orElse(List.of())) {
			rows.add(Stream.concat(Stream.of(
					row.getMaterialName(),
					PartRoleNames.of(row.getRole(), language),
					String.valueOf(row.getQuantity()),
					number(row.getHeightMm()),
					number(row.getWidthMm()),
					"",
					"",
					"",
					"",
					"",
					yes(true, language),
					String.join(" ", row.getCabinetLabels())), imperial(document, row.getHeightMm(), row.getWidthMm()))
					.toList());
		}
		return rows;
	}

	private static List<List<String>> nesting(OutputDocument document) {
		var language = document.language();
		var rows = new ArrayList<List<String>>();
		rows.add(List.of(
				language.pick("Matériau", "Material"),
				language.pick("Panneau", "Sheet"),
				language.pick("Pièce", "Part"),
				language.pick("Caisson", "Cabinet"),
				language.pick("Position en longueur (mm)", "Along (mm)"),
				language.pick("Position en largeur (mm)", "Across (mm)"),
				language.pick("Longueur (mm)", "Length (mm)"),
				language.pick("Largeur (mm)", "Width (mm)"),
				language.pick("Tournée", "Rotated")));
		for (var nesting : document.output().getNesting()) {
			for (int sheet = 0; sheet < nesting.getSheets().size(); sheet++) {
				for (var placement : nesting.getSheets().get(sheet).getPlacements()) {
					rows.add(List.of(
							nesting.getMaterialName(),
							String.valueOf(sheet + 1),
							PartRoleNames.of(placement.getRole(), language),
							Optional.ofNullable(placement.getCabinetLabel()).orElse(""),
							number(placement.getAlongMm()),
							number(placement.getAcrossMm()),
							number(placement.getLengthMm()),
							number(placement.getWidthMm()),
							yes(placement.isRotated(), language)));
				}
			}
		}
		return rows;
	}

	private static List<List<String>> billOfMaterials(OutputDocument document) {
		var language = document.language();
		var rows = new ArrayList<List<String>>();
		rows.add(List.of(
				language.pick("Type", "Kind"),
				language.pick("Article", "Item"),
				language.pick("Code", "SKU"),
				language.pick("Quantité", "Quantity"),
				language.pick("Unité", "Unit"),
				language.pick("Prix unitaire", "Unit price"),
				language.pick("Total", "Total")));
		for (var line : document.output().getBillOfMaterials()) {
			rows.add(List.of(
					kind(line.getKind(), language),
					line.getName(),
					Optional.ofNullable(line.getSku()).orElse(""),
					line.getQuantity().toPlainString(),
					unit(line.getKind(), language),
					line.getUnitPrice().toPlainString(),
					line.getTotal().toPlainString()));
		}
		rows.add(List.of("", language.pick("Total des matériaux", "Materials total"), "", "", "", "",
				Optional.ofNullable(document.output().getMaterialsTotal()).orElse(BigDecimal.ZERO).toPlainString()));
		return rows;
	}

	static String kind(MaterialKind kind, Language language) {
		return switch (kind) {
		case SHEET -> language.pick("Panneau", "Sheet");
		case EDGE_BANDING -> language.pick("Bande de chant", "Edge banding");
		case HARDWARE -> language.pick("Quincaillerie", "Hardware");
		case FRONT -> language.pick("Façades commandées", "Ordered fronts");
		case MOULDING -> language.pick("Moulures", "Mouldings");
		};
	}

	static String unit(MaterialKind kind, Language language) {
		return switch (kind) {
		case SHEET -> language.pick("panneau(x)", "sheet(s)");
		case EDGE_BANDING, MOULDING -> "m";
		case HARDWARE -> language.pick("unité(s)", "unit(s)");
		case FRONT -> "m²";
		};
	}

	private static Stream<String> imperialHeaders(OutputDocument document) {
		var language = document.language();
		return document.units() == Units.IMPERIAL
				? Stream.of(language.pick("Longueur (po)", "Length (in)"), language.pick("Largeur (po)", "Width (in)"))
				: Stream.empty();
	}

	private static Stream<String> imperial(OutputDocument document, double lengthMm, double widthMm) {
		return document.units() == Units.IMPERIAL
				? Stream.of(Lengths.format(lengthMm, document.units()), Lengths.format(widthMm, document.units()))
				: Stream.empty();
	}

	private static String yes(boolean value, Language language) {
		return value ? language.pick("oui", "yes") : language.pick("non", "no");
	}

	private static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	/**
	 * Quotes a field when needed. Text a spreadsheet would run as a formula (names typed by Members
	 * starting with =, +, - or @) is prefixed with an apostrophe so it stays text.
	 */
	static String field(String value) {
		var safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 && !NUMBER.matcher(value).matches()
				? "'" + value
				: value;
		if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
			return "\"" + safe.replace("\"", "\"\"") + "\"";
		}
		return safe;
	}
}
