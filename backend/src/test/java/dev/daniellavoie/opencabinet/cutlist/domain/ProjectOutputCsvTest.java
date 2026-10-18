package dev.daniellavoie.opencabinet.cutlist.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class ProjectOutputCsvTest {
	private static final ProjectOutput OUTPUT = SampleKitchen.output();

	@Test
	void theCutListGivesMillimetresAndTheShopsUnitsWithNamesInTheMembersLanguage() {
		var lines = lines(OutputDocument.CUT_LIST, DrawingLanguage.FR);

		assertThat(lines.get(0)).isEqualTo("﻿part,material,length_mm,width_mm,thickness_mm,length,width,thickness,"
				+ "grain,banded_length_edges,banded_width_edges,edge_banding,quantity,cabinets,ordered");
		assertThat(lines).hasSize(1 + OUTPUT.getCutList().size() + OUTPUT.getOrderedFronts().size());
		// Inch marks are quotes, so lengths in inches are quoted and their marks doubled.
		var back = OUTPUT.getCutList().get(0);
		assertThat(lines.get(1)).isEqualTo("Dos,Back,2184.4,609.6,6.4,\"86\"\"\",\"24\"\"\",\"1/4\"\"\",false,0,0,,"
				+ back.getQuantity() + ",C9,false");
	}

	@Test
	void nestingListsEveryPlacedPartBySheet() {
		var lines = lines(OutputDocument.NESTING, DrawingLanguage.EN);
		var placed = OUTPUT.getNesting().stream()
				.flatMap(material -> material.getSheets().stream())
				.mapToInt(sheet -> sheet.getPlacements().size())
				.sum();

		assertThat(lines.get(0)).endsWith("material,sheet,part,cabinet,along_mm,across_mm,length_mm,width_mm,rotated");
		assertThat(lines).hasSize(1 + placed);
		assertThat(lines.get(1)).startsWith(OUTPUT.getNesting().get(0).getMaterialName() + ",1,");
	}

	@Test
	void theBillOfMaterialsKeepsPricesPlain() {
		var lines = lines(OutputDocument.BILL_OF_MATERIALS, DrawingLanguage.EN);

		assertThat(lines.get(0)).endsWith("kind,item,sku,quantity,unit_price,total");
		assertThat(lines).hasSize(1 + OUTPUT.getBillOfMaterials().size());
		assertThat(lines.get(1)).startsWith("Sheet,Carcass,,");
	}

	@Test
	void fieldsWithCommasOrQuotesAreQuoted() {
		assertThat(ProjectOutputCsv.quote("Érable, 3/4\"")).isEqualTo("\"Érable, 3/4\"\"\"");
		assertThat(ProjectOutputCsv.quote("Carcass")).isEqualTo("Carcass");
	}

	@Test
	void nestingSheetsAreDrawnOneViewPerSheetWithLabelledParts() {
		var views = NestingDrawings.sheets(OUTPUT.getNesting(), Units.IMPERIAL, DrawingLanguage.FR);

		assertThat(views).hasSize(OUTPUT.getOperations().getSheets());
		assertThat(views.get(0).getTitle()).startsWith(OUTPUT.getNesting().get(0).getMaterialName() + " — panneau 1 / ");
		assertThat(views.get(0).getShapes()).filteredOn(Label.class::isInstance)
				.extracting(shape -> ((Label) shape).text())
				.anyMatch(text -> text.endsWith(" Côté"));
	}

	private static List<String> lines(OutputDocument document, DrawingLanguage language) {
		return ProjectOutputCsv.write(document, OUTPUT, Units.IMPERIAL, language).lines().toList();
	}
}
