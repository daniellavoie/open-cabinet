package dev.daniellavoie.opencabinet.drawing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.cutlist.domain.SampleKitchen;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Dimension;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Label;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape.Outline;
import dev.daniellavoie.opencabinet.drawing.domain.model.Stroke;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class QuotePicturesGeneratorTest {
	private static final List<View> PICTURES = QuotePicturesGenerator
			.generate(ShopDrawingsSamples.source(DrawingLanguage.FR, Units.IMPERIAL));

	@Test
	void theRoomIn3dThenEachWallWithCabinets() {
		// Wall 3 has only a doorway.
		assertThat(PICTURES).extracting(View::getTitle)
				.containsExactly("Vue d'ensemble", "Élévation — Mur 1", "Élévation — Mur 2");
	}

	@Test
	void theOverviewShowsEveryCabinetAsASolidTurnedToTheReader() {
		var overview = PICTURES.get(0).getShapes();

		// A top for each Cabinet, Appliance and Filler.
		assertThat(strokes(overview, Stroke.FACE_LIGHT) - walls(overview)).isEqualTo(9 + 2 + 1);
		// Looking into the corner of Walls 1 and 2, both runs show their fronts; Wall 3 is behind the reader.
		assertThat(strokes(overview, Stroke.FACE_MEDIUM)).isEqualTo(9 + 2 + 1);
		assertThat(walls(overview)).isEqualTo(2);
		assertThat(strokes(overview, Stroke.OPENING)).isEqualTo(1);
	}

	@Test
	void clientElevationsGiveOnlyTheWallsSizeAndNoCabinetLabels() {
		var elevation = PICTURES.get(1).getShapes();

		assertThat(elevation).filteredOn(Dimension.class::isInstance)
				.extracting(shape -> ((Dimension) shape).text())
				.containsExactly("144\"", "96\"");
		assertThat(elevation).filteredOn(Label.class::isInstance)
				.extracting(shape -> ((Label) shape).text())
				.containsExactlyInAnyOrder("Fenêtre", "Lave-vaisselle");
	}

	@Test
	void noPicturesWithoutCabinets() {
		var source = ShopDrawingsSamples.source(DrawingLanguage.FR, Units.IMPERIAL);
		var empty = new ShopDrawingsGenerator.Source(Design.builder().walls(SampleKitchen.design().getWalls())
				.cabinets(List.of()).build(), source.templates(), source.labels(), source.parts(), source.materials(),
				source.standard(), source.units(), source.language());

		assertThat(QuotePicturesGenerator.generate(empty)).isEmpty();
	}

	@Test
	void picturesAreStoredAndReadBackUnchanged() throws Exception {
		var mapper = new ObjectMapper();
		var json = mapper.writeValueAsString(PICTURES);

		List<View> read = mapper.readValue(json,
				mapper.getTypeFactory().constructCollectionType(List.class, View.class));
		assertThat(read).isEqualTo(PICTURES);
		assertThat(json).contains("\"type\":\"outline\"", "\"type\":\"dimension\"");
	}

	/**
	 * Walls drawn in 3D: light faces as tall as a Wall.
	 */
	private static long walls(List<Shape> shapes) {
		return shapes.stream()
				.filter(shape -> shape instanceof Outline outline && outline.stroke() == Stroke.FACE_LIGHT
						&& outline.points().size() == 4 && height(outline) > 2000)
				.count();
	}

	private static double height(Outline outline) {
		var heights = outline.points().stream().mapToDouble(Shape.Point::y).summaryStatistics();
		return heights.getMax() - heights.getMin();
	}

	private static long strokes(List<Shape> shapes, Stroke stroke) {
		return shapes.stream().filter(shape -> shape instanceof Outline outline && outline.stroke() == stroke).count();
	}
}
