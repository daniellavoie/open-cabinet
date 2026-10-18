package dev.daniellavoie.opencabinet.drawing.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.SampleKitchen;
import dev.daniellavoie.opencabinet.drawing.domain.model.DrawingLanguage;
import dev.daniellavoie.opencabinet.drawing.domain.model.ShopDrawings;
import dev.daniellavoie.opencabinet.drawing.domain.model.View;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * The Shop Drawings of {@link SampleKitchen}.
 */
public final class ShopDrawingsSamples {
	private ShopDrawingsSamples() {
	}

	static ShopDrawingsGenerator.Source source(DrawingLanguage language, Units units) {
		var output = SampleKitchen.output();
		return new ShopDrawingsGenerator.Source(SampleKitchen.design(),
				SampleKitchen.templates().stream().collect(Collectors.toMap(CabinetTemplate::key, Function.identity())),
				output.getCabinetLabels(), output.getParts(),
				SampleKitchen.catalog().stream().collect(Collectors.toMap(Material::getId, Function.identity())),
				SampleKitchen.standard(), units, language);
	}

	/**
	 * The pictures a Quote of the kitchen prints.
	 */
	public static List<View> pictures(DrawingLanguage language, Units units) {
		return QuotePicturesGenerator.generate(source(language, units));
	}

	public static ShopDrawings sample(DrawingLanguage language, Units units) {
		return ShopDrawings.builder()
				.shopName("Ébénisterie Tremblay")
				.projectName("Cuisine Tremblay")
				.clientName("Marie Tremblay")
				.date(LocalDate.of(2026, 10, 17))
				.units(units)
				.language(language)
				.sheets(ShopDrawingsGenerator.generate(source(language, units)))
				.build();
	}
}
