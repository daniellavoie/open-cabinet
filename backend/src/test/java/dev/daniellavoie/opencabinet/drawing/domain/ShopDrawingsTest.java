package dev.daniellavoie.opencabinet.drawing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.drawing.domain.model.Drawing;
import dev.daniellavoie.opencabinet.drawing.domain.model.Shape;
import dev.daniellavoie.opencabinet.drawing.module.adapter.svg.SvgCanvas;

class ShopDrawingsTest {
	@Test
	void drawsAPlanAnElevationPerWallAndADetailPerCabinet() throws IOException {
		var drawings = ShopDrawings.all(DrawingFixtures.context(Language.FR));

		assertThat(drawings).extracting(Drawing::title).startsWith("Plan", "Élévation — Mur 1", "Élévation — Mur 2")
				.contains("C1 — Bas, tiroirs", "C7 — Colonne garde-manger");
		assertThat(drawings).hasSize(1 + 2 + 7);

		var output = Path.of("target", "drawings");
		Files.createDirectories(output);
		for (int index = 0; index < drawings.size(); index++) {
			var svg = SvgCanvas.render(drawings.get(index));
			assertThat(svg).startsWith("<svg").endsWith("</svg>");
			Files.writeString(output.resolve(index + ".svg"), svg);
		}
	}

	@Test
	void dimensionsTheWallsInTheShopsUnits() {
		var elevation = ShopDrawings.elevation(DrawingFixtures.context(Language.EN), 0);

		assertThat(elevation.title()).isEqualTo("Elevation — Wall 1");
		assertThat(elevation.shapes()).filteredOn(Shape.Dimension.class::isInstance)
				.extracting(shape -> ((Shape.Dimension) shape).label())
				.contains("144\"", "96\"", "36\"", "18\"", "42\"", "34 1/2\"");
	}


	@Test
	void drawsTheRoomIn3d() throws IOException {
		var view = IsometricView.draw(DrawingFixtures.context(Language.FR));

		assertThat(view.title()).isEqualTo("Vue 3D");
		assertThat(view.shapes()).hasSizeGreaterThan(50);
		Files.createDirectories(Path.of("target", "drawings"));
		Files.writeString(Path.of("target", "drawings", "iso.svg"), SvgCanvas.render(view));
	}
}
