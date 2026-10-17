package dev.daniellavoie.opencabinet.catalog.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.daniellavoie.opencabinet.catalog.domain.model.CatalogImportResult.ImportError;
import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialRequest;

/**
 * Reads and writes the Material Catalog as CSV:
 *
 * <pre>
 * kind,name,sku,price,thickness_mm,length_mm,width_mm,grain
 * </pre>
 *
 * Files saved by a French-locale spreadsheet (semicolons between fields, decimal commas) are read
 * too.
 */
final class CatalogCsv {
	static final List<String> COLUMNS = List.of("kind", "name", "sku", "price", "thickness_mm", "length_mm",
			"width_mm", "grain");

	record Row(int line, MaterialRequest request) {
	}

	record Parsed(List<Row> rows, List<ImportError> errors) {
	}

	private CatalogCsv() {
	}

	static String write(List<Material> materials) {
		var csv = new StringBuilder(String.join(",", COLUMNS)).append("\r\n");
		for (var material : materials) {
			csv.append(String.join(",", List.of(
					material.getKind().name(),
					quote(material.getName()),
					quote(Optional.ofNullable(material.getSku()).orElse("")),
					material.getPrice().toPlainString(),
					number(material.getThicknessMm()),
					number(material.getLengthMm()),
					number(material.getWidthMm()),
					Boolean.toString(material.isGrain()))))
					.append("\r\n");
		}
		return csv.toString();
	}

	/**
	 * Parses every data line, collecting a message per bad line rather than stopping at the first.
	 */
	static Parsed read(String csv) {
		var lines = csv.replace("﻿", "").lines().toList();
		var rows = new ArrayList<Row>();
		var errors = new ArrayList<ImportError>();

		if (lines.isEmpty() || lines.get(0).isBlank()) {
			errors.add(error(1, "the file is empty"));
			return new Parsed(rows, errors);
		}

		var header = lines.get(0);
		var separator = header.contains(";") && !header.contains(",") ? ';' : ',';
		var decimalComma = separator == ';';
		var names = split(header, separator).stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
		Map<String, Integer> index = IntStream.range(0, names.size()).boxed()
				.collect(Collectors.toMap(names::get, position -> position, (first, second) -> first));
		var missing = List.of("kind", "name", "price").stream().filter(column -> !index.containsKey(column)).toList();
		if (!missing.isEmpty()) {
			errors.add(error(1, "missing column(s): " + String.join(", ", missing)));
			return new Parsed(rows, errors);
		}

		for (int number = 2; number <= lines.size(); number++) {
			var line = lines.get(number - 1);
			if (line.isBlank()) {
				continue;
			}
			var fields = split(line, separator);
			try {
				rows.add(new Row(number, MaterialRequest.builder()
						.kind(kind(field(fields, index, "kind")))
						.name(field(fields, index, "name"))
						.sku(Optional.ofNullable(field(fields, index, "sku")).filter(sku -> !sku.isBlank()).orElse(null))
						.price(decimal(field(fields, index, "price"), decimalComma, "price"))
						.thicknessMm(millimetres(field(fields, index, "thickness_mm"), decimalComma, "thickness_mm"))
						.lengthMm(millimetres(field(fields, index, "length_mm"), decimalComma, "length_mm"))
						.widthMm(millimetres(field(fields, index, "width_mm"), decimalComma, "width_mm"))
						.grain(bool(field(fields, index, "grain")))
						.build()));
			} catch (IllegalArgumentException exception) {
				errors.add(error(number, exception.getMessage()));
			}
		}

		return new Parsed(rows, errors);
	}

	static ImportError error(int line, String message) {
		return ImportError.builder().line(line).message(message).build();
	}

	private static String field(List<String> fields, Map<String, Integer> index, String column) {
		var position = index.get(column);
		if (position == null || position >= fields.size()) {
			return null;
		}
		var value = fields.get(position).trim();
		return value.isEmpty() ? null : value;
	}

	private static MaterialKind kind(String value) {
		try {
			return MaterialKind.valueOf(Optional.ofNullable(value).orElse("").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("kind must be SHEET, EDGE_BANDING, HARDWARE, FRONT or MOULDING");
		}
	}

	private static BigDecimal decimal(String value, boolean decimalComma, String column) {
		if (value == null) {
			return null;
		}
		var normalized = decimalComma ? value.replace(" ", "").replace(',', '.') : value;
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(column + " must be a number");
		}
	}

	private static Double millimetres(String value, boolean decimalComma, String column) {
		return Optional.ofNullable(decimal(value, decimalComma, column)).map(BigDecimal::doubleValue).orElse(null);
	}

	private static boolean bool(String value) {
		return value != null && List.of("true", "yes", "oui", "1", "vrai").contains(value.toLowerCase(Locale.ROOT));
	}

	/**
	 * Splits one CSV line, honouring double-quoted fields and doubled quotes inside them.
	 */
	static List<String> split(String line, char separator) {
		var fields = new ArrayList<String>();
		var current = new StringBuilder();
		var quoted = false;
		for (int position = 0; position < line.length(); position++) {
			var character = line.charAt(position);
			if (quoted) {
				if (character == '"' && position + 1 < line.length() && line.charAt(position + 1) == '"') {
					current.append('"');
					position++;
				} else if (character == '"') {
					quoted = false;
				} else {
					current.append(character);
				}
			} else if (character == '"') {
				quoted = true;
			} else if (character == separator) {
				fields.add(current.toString());
				current.setLength(0);
			} else {
				current.append(character);
			}
		}
		fields.add(current.toString());
		return fields;
	}

	private static String quote(String value) {
		return value.contains(",") || value.contains("\"") || value.contains("\n")
				? "\"" + value.replace("\"", "\"\"") + "\""
				: value;
	}

	private static String number(Double value) {
		return value == null ? "" : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}
}
