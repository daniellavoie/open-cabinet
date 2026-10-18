package dev.daniellavoie.opencabinet.quote.module.adapter.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import dev.daniellavoie.opencabinet.drawing.domain.ShopDrawingsSamples;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteContent;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLine;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLineKind;
import dev.daniellavoie.opencabinet.quote.domain.model.QuotePictures;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import dev.daniellavoie.opencabinet.quote.domain.model.TaxLine;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class OpenPdfQuoteRendererTest {
	private final OpenPdfQuoteRenderer renderer = new OpenPdfQuoteRenderer();

	@Test
	void printsTheRoomIn3dThenThePriceThenTheElevations() throws IOException {
		var pdf = renderer.pdf(quote().toBuilder()
				.pictures(QuotePictures.builder()
						.views(ShopDrawingsSamples.pictures(DrawingLanguage.FR, Units.IMPERIAL))
						.build())
				.build());
		// Kept for a look at the pictures: target/quote-sample.pdf.
		Files.write(Path.of("target", "quote-sample.pdf"), pdf);

		var reader = new PdfReader(pdf);
		var extractor = new PdfTextExtractor(reader);
		var text = new StringBuilder();
		for (int page = 1; page <= reader.getNumberOfPages(); page++) {
			text.append(extractor.getTextFromPage(page)).append('\n');
		}
		var all = text.toString();
		assertThat(all.indexOf("Total")).isPositive().isLessThan(all.indexOf("Élévation — Mur 1"));
		assertThat(all).contains("Élévation — Mur 2", "144\"");
	}

	@Test
	void quotesFromBeforePicturesPrintAsBefore() throws IOException {
		var reader = new PdfReader(renderer.pdf(quote()));

		assertThat(reader.getNumberOfPages()).isEqualTo(1);
		assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Soumission S-0042", "Total");
	}

	private static Quote quote() {
		return Quote.builder()
				.id(UUID.randomUUID())
				.projectId(UUID.randomUUID())
				.number(42)
				.status(QuoteStatus.DRAFT)
				.creationTimestamp(LocalDateTime.of(2026, 10, 18, 9, 0))
				.content(QuoteContent.builder()
						.shopName("Ébénisterie Tremblay")
						.clientName("Marie Tremblay")
						.projectName("Cuisine Tremblay")
						.currency("CAD")
						.cabinetCount(9)
						.lines(List.of(QuoteLine.builder().kind(QuoteLineKind.CABINETS)
								.description("Armoires sur mesure : 9 caisson(s)").amount(new BigDecimal("18450.00")).build()))
						.subtotal(new BigDecimal("18450.00"))
						.taxes(List.of(TaxLine.builder().name("TPS").ratePercent(new BigDecimal("5"))
								.amount(new BigDecimal("922.50")).build()))
						.total(new BigDecimal("19372.50"))
						.build())
				.build();
	}
}
