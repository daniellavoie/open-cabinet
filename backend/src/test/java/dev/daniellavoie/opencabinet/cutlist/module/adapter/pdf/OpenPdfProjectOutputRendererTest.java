package dev.daniellavoie.opencabinet.cutlist.module.adapter.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import dev.daniellavoie.opencabinet.cutlist.domain.PrintedOutputSamples;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

class OpenPdfProjectOutputRendererTest {
	private final OpenPdfProjectOutputRenderer renderer = new OpenPdfProjectOutputRenderer();

	@Test
	void printsTheCutListWithTheFrontsToOrder() throws IOException {
		var text = text(OutputDocument.CUT_LIST, DrawingLanguage.FR);

		assertThat(text).contains("Liste de coupe", "Ébénisterie Tremblay", "Façade de tiroir", "Page 1");
	}

	@Test
	void printsAPagePerNestedSheet() throws IOException {
		var printed = PrintedOutputSamples.sample(OutputDocument.NESTING, DrawingLanguage.EN, Units.IMPERIAL);
		var reader = new PdfReader(pdf(OutputDocument.NESTING, DrawingLanguage.EN));

		assertThat(reader.getNumberOfPages()).isEqualTo(printed.getSheets().size());
		assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Nesting", "sheet 1 /", "Scale");
	}

	@Test
	void printsTheBillOfMaterialsWithItsTotal() throws IOException {
		assertThat(text(OutputDocument.BILL_OF_MATERIALS, DrawingLanguage.EN))
				.contains("Bill of materials", "Materials total", "Hardware");
	}

	private String text(OutputDocument document, DrawingLanguage language) throws IOException {
		var reader = new PdfReader(pdf(document, language));
		var extractor = new PdfTextExtractor(reader);
		var text = new StringBuilder();
		for (int page = 1; page <= reader.getNumberOfPages(); page++) {
			text.append(extractor.getTextFromPage(page)).append('\n');
		}
		return text.toString();
	}

	private byte[] pdf(OutputDocument document, DrawingLanguage language) throws IOException {
		var pdf = renderer.pdf(PrintedOutputSamples.sample(document, language, Units.IMPERIAL));
		// Kept for a look: target/<document>-sample.pdf.
		Files.write(Path.of("target", document.slug() + "-sample.pdf"), pdf);
		return pdf;
	}
}
