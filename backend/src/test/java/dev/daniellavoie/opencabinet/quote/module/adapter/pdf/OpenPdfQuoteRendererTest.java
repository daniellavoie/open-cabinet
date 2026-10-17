package dev.daniellavoie.opencabinet.quote.module.adapter.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.drawing.domain.DrawingFixtures;
import dev.daniellavoie.opencabinet.drawing.domain.IsometricView;
import dev.daniellavoie.opencabinet.drawing.domain.ShopDrawings;
import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteContent;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLine;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLineKind;
import dev.daniellavoie.opencabinet.quote.domain.model.QuotePictures;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;

class OpenPdfQuoteRendererTest {
	private static final Quote QUOTE = Quote.builder()
			.id(UUID.randomUUID())
			.number(42)
			.status(QuoteStatus.SENT)
			.creationTimestamp(LocalDateTime.of(2026, 10, 17, 9, 0))
			.content(QuoteContent.builder()
					.shopName("Atelier Rosemont")
					.clientName("Jean Tremblay")
					.projectName("Cuisine")
					.currency("CAD")
					.lines(List.of(QuoteLine.builder().kind(QuoteLineKind.CABINETS).description("Armoires sur mesure")
							.amount(new BigDecimal("12000.00")).build()))
					.subtotal(new BigDecimal("12000.00"))
					.taxes(List.of())
					.total(new BigDecimal("12000.00"))
					.build())
			.build();

	@Test
	void addsPicturesOfTheDesignAfterThePrice() throws IOException {
		var context = DrawingFixtures.context(Language.FR);
		var pictures = new QuotePictures(IsometricView.draw(context), ShopDrawings.plan(context),
				List.of(ShopDrawings.elevation(context, 0), ShopDrawings.elevation(context, 1)));

		var pdf = new OpenPdfQuoteRenderer().pdf(QUOTE, pictures);

		assertThat(pages(pdf)).isEqualTo(1 + 2);
		Files.createDirectories(Path.of("target", "drawings"));
		Files.write(Path.of("target", "drawings", "quote.pdf"), pdf);
	}

	@Test
	void keepsQuotesWithoutPicturesToOnePage() {
		assertThat(pages(new OpenPdfQuoteRenderer().pdf(QUOTE, QuotePictures.NONE))).isEqualTo(1);
	}

	private static int pages(byte[] pdf) {
		var text = new String(pdf, StandardCharsets.ISO_8859_1);
		var marker = text.indexOf("/Count ");
		return Integer.parseInt(text.substring(marker + 7).split("[^0-9]")[0]);
	}
}
