package dev.daniellavoie.opencabinet.cutlist.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.drawing.domain.LengthFormat;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Writes a Project's Cut List, Nesting or Bill of Materials as CSV, the way the Material Catalog is
 * exported: commas, decimal points, column names that never change with the language, lengths in
 * millimetres. Cut List lengths are also given as the Shop measures them (34 1/2"), and names are in
 * the Member's language. Starts with a byte order mark so spreadsheets read accents right.
 */
final class ProjectOutputCsv {
	static final String BYTE_ORDER_MARK = "﻿";

	private ProjectOutputCsv() {
	}

	static String write(OutputDocument document, ProjectOutput output, Units units, DrawingLanguage language) {
		var rows = switch (document) {
		case CUT_LIST -> cutList(output, units, language);
		case NESTING -> nesting(output, language);
		case BILL_OF_MATERIALS -> billOfMaterials(output, language);
		};
		var csv = new StringBuilder(BYTE_ORDER_MARK);
		rows.forEach(row -> csv.append(String.join(",", row.stream().map(ProjectOutputCsv::quote).toList()))
				.append("\r\n"));
		return csv.toString();
	}

	/**
	 * Cut Parts, then Fronts to order (marked ordered, with no thickness or banding).
	 */
	private static List<List<String>> cutList(ProjectOutput output, Units units, DrawingLanguage language) {
		var rows = new ArrayList<List<String>>();
		rows.add(List.of("part", "material", "length_mm", "width_mm", "thickness_mm", "length", "width",
				"thickness", "grain", "banded_length_edges", "banded_width_edges", "edge_banding", "quantity",
				"cabinets", "ordered"));
		for (var row : output.getCutList()) {
			rows.add(List.of(language.partRole(row.getRole()), row.getMaterialName(), number(row.getLengthMm()),
					number(row.getWidthMm()), number(row.getThicknessMm()), LengthFormat.format(row.getLengthMm(), units),
					LengthFormat.format(row.getWidthMm(), units), LengthFormat.format(row.getThicknessMm(), units),
					Boolean.toString(row.isGrain()), Integer.toString(row.getBandedLengthEdges()),
					Integer.toString(row.getBandedWidthEdges()), Optional.ofNullable(row.getEdgeBandingName()).orElse(""),
					Integer.toString(row.getQuantity()), String.join(" ", row.getCabinetLabels()), "false"));
		}
		for (var row : Optional.ofNullable(output.getOrderedFronts()).orElse(List.of())) {
			rows.add(List.of(language.partRole(row.getRole()), row.getMaterialName(), number(row.getHeightMm()),
					number(row.getWidthMm()), "", LengthFormat.format(row.getHeightMm(), units),
					LengthFormat.format(row.getWidthMm(), units), "", "false", "", "", "",
					Integer.toString(row.getQuantity()), String.join(" ", row.getCabinetLabels()), "true"));
		}
		return rows;
	}

	/**
	 * One line per Part placed, sheet by sheet; Parts too big for their sheet with no sheet number.
	 */
	private static List<List<String>> nesting(ProjectOutput output, DrawingLanguage language) {
		var rows = new ArrayList<List<String>>();
		rows.add(List.of("material", "sheet", "part", "cabinet", "along_mm", "across_mm", "length_mm", "width_mm",
				"rotated"));
		for (var material : output.getNesting()) {
			var sheets = material.getSheets();
			for (int index = 0; index < sheets.size(); index++) {
				for (var placement : sheets.get(index).getPlacements()) {
					rows.add(List.of(material.getMaterialName(), Integer.toString(index + 1),
							language.partRole(placement.getRole()), placement.getCabinetLabel(),
							number(placement.getAlongMm()), number(placement.getAcrossMm()),
							number(placement.getLengthMm()), number(placement.getWidthMm()),
							Boolean.toString(placement.isRotated())));
				}
			}
			for (var part : Optional.ofNullable(material.getOversizedParts()).orElse(List.of())) {
				rows.add(List.of(material.getMaterialName(), "", language.partRole(part.getRole()),
						Optional.ofNullable(part.getCabinetLabel()).orElse(""), "", "", number(part.getLengthMm()),
						number(part.getWidthMm()), ""));
			}
		}
		return rows;
	}

	private static List<List<String>> billOfMaterials(ProjectOutput output, DrawingLanguage language) {
		var rows = new ArrayList<List<String>>();
		rows.add(List.of("kind", "item", "sku", "quantity", "unit_price", "total"));
		for (var line : output.getBillOfMaterials()) {
			rows.add(List.of(language.materialKind(line.getKind()), line.getName(),
					Optional.ofNullable(line.getSku()).orElse(""), plain(line.getQuantity()), plain(line.getUnitPrice()),
					plain(line.getTotal())));
		}
		return rows;
	}

	private static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	private static String plain(BigDecimal value) {
		return value == null ? "" : value.toPlainString();
	}

	/**
	 * Quotes a field holding a comma, a quote or a line break, doubling its quotes.
	 */
	static String quote(String value) {
		return value.contains(",") || value.contains("\"") || value.contains("\n")
				? "\"" + value.replace("\"", "\"\"") + "\""
				: value;
	}
}
