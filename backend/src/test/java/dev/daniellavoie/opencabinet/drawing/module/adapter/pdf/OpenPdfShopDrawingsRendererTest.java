package dev.daniellavoie.opencabinet.drawing.module.adapter.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import dev.daniellavoie.opencabinet.drawing.domain.ShopDrawingsSamples;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class OpenPdfShopDrawingsRendererTest {
	private final OpenPdfShopDrawingsRenderer renderer = new OpenPdfShopDrawingsRenderer();

	@Test
	void printsASheetPerPageWithItsTitleBlockAndScale() throws IOException {
		var pdf = renderer.pdf(ShopDrawingsSamples.sample(DrawingLanguage.FR, Units.IMPERIAL));
		// Kept for a look at the drawings: target/shop-drawings-sample.pdf.
		Files.write(Path.of("target", "shop-drawings-sample.pdf"), pdf);

		var reader = new PdfReader(pdf);
		assertThat(reader.getNumberOfPages()).isEqualTo(13);

		var extractor = new PdfTextExtractor(reader);
		assertThat(extractor.getTextFromPage(1)).contains("Vue en plan", "Ébénisterie Tremblay", "Feuille 1 / 13");
		assertThat(extractor.getTextFromPage(2)).contains("Élévation — Mur 1", "Échelle 1/2\" = 1'-0\"");
		assertThat(extractor.getTextFromPage(8)).contains("C4 — Bas, tiroirs", "Façade de tiroir");
	}

	@Test
	void metricShopsGetMetricScales() throws IOException {
		var pdf = renderer.pdf(ShopDrawingsSamples.sample(DrawingLanguage.EN, Units.METRIC));

		var text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(2);
		assertThat(text).contains("Elevation — Wall 1", "Scale 1:");
	}
}
