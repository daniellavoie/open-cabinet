package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.cutlist.domain.model.CutListRow;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.drawing.domain.DrawingFixtures;
import dev.daniellavoie.opencabinet.drawing.domain.ShopDrawings;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSet;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawingSheet;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class OpenPdfShopDrawingsRendererTest {
	@Test
	void printsOnePagePerDrawing() throws IOException {
		var context = DrawingFixtures.context(Language.FR);
		var parts = List.of(CutListRow.builder().role(PartRole.SIDE).materialId(UUID.randomUUID())
				.materialName("Mélamine blanche 5/8\"").lengthMm(774.7).widthMm(587.3).quantity(2)
				.cabinetLabels(List.of("C1")).build());
		var sheets = ShopDrawings.all(context).stream()
				.map(drawing -> new ShopDrawingSheet(ShopDrawingSheet.Kind.PLAN, drawing, drawing.title().startsWith("C") ? parts : List.of()))
				.toList();

		var pdf = new OpenPdfShopDrawingsRenderer().pdf(new ShopDrawingSet("Atelier Rosemont", "Cuisine",
				"Jean Tremblay", Units.IMPERIAL, Language.FR, sheets));

		assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
		assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("/Count " + sheets.size());
		Files.createDirectories(Path.of("target", "drawings"));
		Files.write(Path.of("target", "drawings", "shop-drawings.pdf"), pdf);
	}
}
