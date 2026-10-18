package dev.daniellavoie.opencabinet.cutlist.domain;

import java.time.LocalDate;
import java.util.List;

import dev.daniellavoie.opencabinet.cutlist.domain.model.OutputDocument;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PrintedOutput;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * {@link SampleKitchen}'s outputs, ready to print.
 */
public final class PrintedOutputSamples {
	private PrintedOutputSamples() {
	}

	public static PrintedOutput sample(OutputDocument document, DrawingLanguage language, Units units) {
		var output = SampleKitchen.output();
		return PrintedOutput.builder()
				.document(document)
				.shopName("Ébénisterie Tremblay")
				.projectName("Cuisine Tremblay")
				.clientName("Marie Tremblay")
				.date(LocalDate.of(2026, 10, 18))
				.units(units)
				.currency("CAD")
				.language(language)
				.output(output)
				.sheets(document == OutputDocument.NESTING ? NestingDrawings.sheets(output.getNesting(), units, language)
						: List.of())
				.build();
	}
}
