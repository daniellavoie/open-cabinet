package dev.daniellavoie.opencabinet.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.CatalogImportResult.ImportError;
import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;

class CatalogCsvTest {
	@Test
	void writtenCatalogReadsBack() {
		var materials = List.of(
				Material.builder().id(UUID.randomUUID()).kind(MaterialKind.SHEET).name("Mélamine 5/8\", blanche")
						.sku("MEL-58").price(new BigDecimal("68.00")).thicknessMm(15.9).lengthMm(2463.8).widthMm(1244.6)
						.grain(true).build(),
				Material.builder().id(UUID.randomUUID()).kind(MaterialKind.HARDWARE).name("Charnière")
						.sku("HNG").price(new BigDecimal("6.50")).build());

		var parsed = CatalogCsv.read(CatalogCsv.write(materials));

		assertThat(parsed.errors()).isEmpty();
		assertThat(parsed.rows()).hasSize(2);
		var sheet = parsed.rows().get(0).request();
		assertThat(sheet.getName()).isEqualTo("Mélamine 5/8\", blanche");
		assertThat(sheet.getLengthMm()).isEqualTo(2463.8);
		assertThat(sheet.isGrain()).isTrue();
		assertThat(parsed.rows().get(1).request().getThicknessMm()).isNull();
	}

	@Test
	void readsFrenchSpreadsheetFiles() {
		var parsed = CatalogCsv.read("""
				﻿kind;name;sku;price;thickness_mm;length_mm;width_mm;grain
				SHEET;Merisier;MER-34;142,50;19,05;2438,4;1219,2;oui
				""");

		assertThat(parsed.errors()).isEmpty();
		var request = parsed.rows().get(0).request();
		assertThat(request.getPrice()).isEqualByComparingTo("142.50");
		assertThat(request.getThicknessMm()).isEqualTo(19.05);
		assertThat(request.isGrain()).isTrue();
	}

	@Test
	void reportsEveryBadLine() {
		var parsed = CatalogCsv.read("""
				kind,name,price
				SHEET,Good,10
				BOARD,Bad kind,10
				HARDWARE,Bad price,ten
				""");

		assertThat(parsed.rows()).hasSize(1);
		assertThat(parsed.errors()).extracting(ImportError::getLine).containsExactly(3, 4);
	}

	@Test
	void reportsMissingColumns() {
		assertThat(CatalogCsv.read("name,sku\nA,B").errors())
				.extracting(ImportError::getMessage)
				.containsExactly("missing column(s): kind, price");
	}
}
