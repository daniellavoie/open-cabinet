package dev.daniellavoie.opencabinet.drawing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.cutlist.domain.SampleKitchen;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Sheet;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.project.domain.ConstructionRules;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class ShopDrawingsGeneratorTest {
	private static final List<Sheet> SHEETS = ShopDrawingsGenerator
			.generate(ShopDrawingsSamples.source(DrawingLanguage.FR, Units.IMPERIAL));

	@Test
	void theSampleKitchenIsBuildable() {
		assertThat(ConstructionRules.check(SampleKitchen.design(), SampleKitchen.templates())).isEmpty();
	}

	@Test
	void aPlanThenAnElevationPerWallThenASheetPerCabinetInLabelOrder() {
		assertThat(SHEETS).extracting(Sheet::getTitle).containsExactly(
				"Vue en plan",
				"Élévation — Mur 1", "Élévation — Mur 2", "Élévation — Mur 3",
				"C1 — Bas, portes", "C2 — Haut, portes", "C3 — Bas d'évier", "C4 — Bas, tiroirs",
				"C5 — Haut, portes", "C6 — Bas, coin aveugle", "C7 — Bas, portes", "C8 — Bas, portes",
				"C9 — Colonne garde-manger");
	}

	@Test
	void thePlanShowsWallsWithTheirLengthsAndEveryCabinetLabelled() {
		var shapes = shapes(SHEETS.get(0), 0);

		assertThat(shapes).filteredOn(shape -> shape instanceof Outline outline && outline.stroke() == Stroke.WALL)
				.hasSize(3);
		assertThat(texts(shapes, Dimension.class)).containsExactly("Mur 1 : 144\"", "Mur 2 : 120\"", "Mur 3 : 144\"");
		assertThat(texts(shapes, Label.class)).contains("C1", "C2", "C9", "Lave-vaisselle", "Réfrigérateur");
		// Two wall Cabinets, dashed; the dishwasher and fridge too.
		assertThat(shapes).filteredOn(shape -> shape instanceof Outline outline && outline.stroke() == Stroke.HIDDEN)
				.hasSize(4);
	}

	@Test
	void anElevationChainsWidthsAlongTheFloorAndOverTheWallCabinets() {
		var shapes = shapes(SHEETS.get(1), 0);

		assertThat(texts(shapes, Dimension.class)).containsExactly(
				"18\"", "24\"", "36\"", "24\"", "42\"",
				"144\"", "96\"",
				"30\"", "30\"",
				"54\"");
		assertThat(texts(shapes, Label.class)).contains("Fenêtre", "Lave-vaisselle", "C1", "C6");
		assertThat(SHEETS.get(1).getNotes()).startsWith("C1  Bas, portes  18\" × 34 1/2\" × 24\"");
	}

	@Test
	void aCabinetSheetDimensionsEachFrontRowAndListsItsOwnParts() {
		var drawers = SHEETS.get(7);
		var front = shapes(drawers, 0);

		// Width, height, three drawer fronts and the toe kick.
		assertThat(texts(front, Dimension.class)).hasSize(6).startsWith("24\"", "34 1/2\"").endsWith("4\"");
		assertThat(drawers.getViews().get(1).getTitle()).isEqualTo("Côté droit");
		assertThat(texts(shapes(drawers, 1), Dimension.class)).containsExactly("24\"");

		var output = SampleKitchen.output();
		var ownParts = output.getParts().stream().filter(part -> "drawers".equals(part.getCabinetId())).count();
		assertThat(drawers.getParts().getRows().stream().mapToLong(row -> Long.parseLong(row.get(6))).sum())
				.isEqualTo(ownParts);
		assertThat(drawers.getParts().getRows()).extracting(row -> row.get(0)).contains("Côté", "Façade de tiroir");
	}

	@Test
	void aBlindCornerLeavesItsBlindSideToTheBlindPanel() {
		var corner = shapes(SHEETS.get(9), 0);
		var gap = SampleKitchen.standard().getDoorGapMm();

		// Blind on the right: the blind panel starts 609.6 mm from the right side.
		assertThat(corner).filteredOn(Outline.class::isInstance)
				.map(shape -> ((Outline) shape).points().get(0).x())
				.contains(1066.8 - 609.6 + gap / 2, gap / 2);
	}

	@Test
	void printsInEnglishAndMillimetresWhenAsked() {
		var sheets = ShopDrawingsGenerator.generate(ShopDrawingsSamples.source(DrawingLanguage.EN, Units.METRIC));

		assertThat(sheets.get(1).getTitle()).isEqualTo("Elevation — Wall 1");
		assertThat(texts(shapes(sheets.get(0), 0), Dimension.class)).startsWith("Wall 1 : 3658 mm");
		assertThat(sheets.get(6).getParts().getHeaders()).startsWith("Part", "Material");
	}

	@Test
	void assembledCabinetsAreDrawnFromTheirParts() throws Exception {
		var source = ShopDrawingsSamples.source(DrawingLanguage.FR, Units.IMPERIAL);
		var mapper = new ObjectMapper();
		var assembly = mapper.treeToValue(mapper.readTree(new ClassPathResource("template-assembly/cases.json")
				.getInputStream()).get(0).get("assembly"), TemplateAssembly.class);
		var template = CabinetTemplate.builder().code("TEST_BASE").version(1).type(CabinetType.BASE).assembly(assembly)
				.build();
		var cabinet = SampleKitchen.design().getCabinets().get(0).toBuilder().templateCode("TEST_BASE")
				.dividerPositionsMm(null).build();

		var front = ShopDrawingsGenerator.front(source, cabinet, template, 0, 0);

		// The Cabinet's outline, then what is seen from the front: both sides, the bottom and the
		// stretcher at the carcass' front, and the drawer front and door; not the set-back shelf or the back.
		assertThat(front).hasSize(1 + 6);
		assertThat(front).filteredOn(shape -> shape instanceof Outline outline && outline.stroke() == Stroke.THIN)
				.hasSize(2);
	}

	private static List<Shape> shapes(Sheet sheet, int view) {
		return sheet.getViews().get(view).getShapes();
	}

	private static List<String> texts(List<Shape> shapes, Class<? extends Shape> kind) {
		return shapes.stream().filter(kind::isInstance).map(shape -> switch (shape) {
		case Dimension dimension -> dimension.text();
		case Label label -> label.text();
		default -> "";
		}).toList();
	}
}
