package dev.daniellavoie.opencabinet.cutlist.module.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.cutlist.domain.model.BomLine;
import dev.daniellavoie.opencabinet.cutlist.domain.model.CutListRow;
import dev.daniellavoie.opencabinet.cutlist.domain.model.MaterialNesting;
import dev.daniellavoie.opencabinet.cutlist.domain.model.NestedSheet;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OperationCounts;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OrderedFrontRow;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.cutlist.domain.model.SheetPlacement;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class OutputExportTest {
	private static final ProjectOutput OUTPUT = ProjectOutput.builder()
			.cabinetLabels(Map.of())
			.cutList(List.of(CutListRow.builder().role(PartRole.SIDE).materialId(UUID.randomUUID())
					.materialName("=Mélamine, blanche").lengthMm(774.7).widthMm(587.3).thicknessMm(15.9).grain(false)
					.bandedLengthEdges(1).bandedWidthEdges(0).edgeBandingId(UUID.randomUUID())
					.edgeBandingName("PVC blanc").quantity(2).cabinetLabels(List.of("C1")).build()))
			.orderedFronts(List.of(OrderedFrontRow.builder().role(PartRole.DOOR).materialId(UUID.randomUUID())
					.materialName("Shaker").heightMm(771.7).widthMm(454.2).quantity(1).cabinetLabels(List.of("C1"))
					.build()))
			.nesting(List.of(MaterialNesting.builder().materialId(UUID.randomUUID()).materialName("Mélamine")
					.sheetLengthMm(2463.8).sheetWidthMm(1244.6).grain(false)
					.sheets(List.of(NestedSheet.builder().utilization(0.3).placements(List.of(
							SheetPlacement.builder().cabinetLabel("C1").role(PartRole.SIDE).alongMm(10).acrossMm(10)
									.lengthMm(774.7).widthMm(587.3).build(),
							SheetPlacement.builder().cabinetLabel("C1").role(PartRole.SIDE).alongMm(788.7).acrossMm(10)
									.lengthMm(774.7).widthMm(587.3).build()))
							.build()))
					.oversizedParts(List.of())
					.build()))
			.billOfMaterials(List.of(BomLine.builder().materialId(UUID.randomUUID()).kind(MaterialKind.SHEET)
					.name("Mélamine").sku("MEL").quantity(BigDecimal.ONE).unitPrice(new BigDecimal("68.00"))
					.total(new BigDecimal("68.00")).build()))
			.materialsTotal(new BigDecimal("68.00"))
			.operations(OperationCounts.builder().build())
			.build();

	private static OutputDocument document(Language language) {
		return new OutputDocument("Atelier", "Cuisine", "Jean Tremblay", Units.IMPERIAL, "CAD", language, OUTPUT);
	}

	@Test
	void writesTheCutListAsCsvWithImperialLengths() {
		var csv = new String(OutputCsv.write(document(Language.EN), OutputKind.CUT_LIST), StandardCharsets.UTF_8);
		var lines = csv.substring(1).split("\r\n");

		assertThat(csv).startsWith("﻿");
		assertThat(lines[0]).startsWith("Material,Part,Qty,Length (mm)").endsWith("Length (in),Width (in)");
		assertThat(lines[1]).startsWith("\"'=Mélamine, blanche\",Side,2,774.7,587.3,15.9,no,1,0,PVC blanc,no,C1")
				.endsWith("\"30 1/2\"\"\",\"23 1/8\"\"\"");
		assertThat(lines[2]).startsWith("Shaker,Door,1,771.7,454.2,,,,,,yes,C1");
	}

	@Test
	void writesNestingPlacementsAndTheBillOfMaterials() {
		var nesting = new String(OutputCsv.write(document(Language.FR), OutputKind.NESTING), StandardCharsets.UTF_8);
		var bom = new String(OutputCsv.write(document(Language.FR), OutputKind.BILL_OF_MATERIALS),
				StandardCharsets.UTF_8);

		assertThat(nesting.lines()).hasSize(3).last().asString().isEqualTo("Mélamine,1,Côté,C1,788.7,10,774.7,587.3,non");
		assertThat(bom.lines()).contains("Panneau,Mélamine,MEL,1,panneau(x),68.00,68.00", ",Total des matériaux,,,,,68.00");
	}

	@Test
	void keepsNumbersAndOnlyDefusesFormulas() {
		assertThat(OutputCsv.field("-12.5")).isEqualTo("-12.5");
		assertThat(OutputCsv.field("@SUM(A1)")).isEqualTo("'@SUM(A1)");
		assertThat(OutputCsv.field("C1")).isEqualTo("C1");
	}

	@Test
	void printsEachOutput() throws IOException {
		Files.createDirectories(Path.of("target", "drawings"));
		for (var kind : OutputKind.values()) {
			var pdf = OutputPdf.write(document(Language.FR), kind);
			assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
			Files.write(Path.of("target", "drawings", kind.fileName(Language.EN) + ".pdf"), pdf);
		}
	}
}
