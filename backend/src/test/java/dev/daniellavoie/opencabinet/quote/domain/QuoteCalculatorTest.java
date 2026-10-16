package dev.daniellavoie.opencabinet.quote.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.cutlist.domain.model.OperationCounts;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.project.domain.model.ManualLine;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLineKind;
import dev.daniellavoie.opencabinet.quote.domain.model.TaxLine;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.ShopSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Tax;

class QuoteCalculatorTest {
	private static final LabourSettings RATES = LabourSettings.builder()
			.defaultMethod(LabourMethod.OPERATION)
			.ratePerSheet(new BigDecimal("35"))
			.ratePerEdgeBandingMeter(new BigDecimal("1.50"))
			.ratePerCabinet(new BigDecimal("60"))
			.ratePerHardwareItem(new BigDecimal("4"))
			.ratePerLinearFoot(new BigDecimal("250"))
			.hourlyRate(new BigDecimal("65"))
			.build();

	private static final Shop SHOP = Shop.builder()
			.name("Atelier")
			.currency("CAD")
			.settings(ShopSettings.builder()
					.markupPercent(new BigDecimal("25"))
					.taxes(List.of(
							Tax.builder().name("TPS").ratePercent(new BigDecimal("5")).build(),
							Tax.builder().name("TVQ").ratePercent(new BigDecimal("9.975")).build()))
					.labour(RATES)
					.build())
			.build();

	private static final ProjectOutput OUTPUT = ProjectOutput.builder()
			.cabinetLabels(Map.of())
			.materialsTotal(new BigDecimal("500.00"))
			.operations(OperationCounts.builder()
					.sheets(4)
					.edgeBandingMeters(new BigDecimal("20.0"))
					.cabinets(3)
					.hardwareItems(10)
					.linearFeet(new BigDecimal("6.00"))
					.build())
			.build();

	private static final Client CLIENT = Client.builder().name("Marie Tremblay").build();

	@Test
	void operationLabourMarkupManualLinesAndTaxes() {
		var project = Project.builder()
				.name("Cuisine")
				.manualLines(List.of(ManualLine.builder().description("Installation").amount(new BigDecimal("400"))
						.build()))
				.build();

		var content = QuoteCalculator.calculate(SHOP, project, CLIENT, OUTPUT);

		// labour = 4×35 + 20×1.50 + 3×60 + 10×4 = 140 + 30 + 180 + 40 = 390
		assertThat(content.getBreakdown().getLabourMethod()).isEqualTo(LabourMethod.OPERATION);
		assertThat(content.getBreakdown().getLabour()).isEqualByComparingTo("390.00");
		// markup = (500 + 390) × 25 % = 222.50
		assertThat(content.getBreakdown().getMarkup()).isEqualByComparingTo("222.50");

		assertThat(content.getLines()).hasSize(2);
		assertThat(content.getLines().get(0).getKind()).isEqualTo(QuoteLineKind.CABINETS);
		assertThat(content.getLines().get(0).getAmount()).isEqualByComparingTo("1112.50");
		assertThat(content.getLines().get(1).getAmount()).isEqualByComparingTo("400.00");

		assertThat(content.getSubtotal()).isEqualByComparingTo("1512.50");
		assertThat(content.getTaxes()).extracting(TaxLine::getAmount)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("75.63"), new BigDecimal("150.87"));
		assertThat(content.getTotal()).isEqualByComparingTo("1739.00");
		assertThat(content.getClientName()).isEqualTo("Marie Tremblay");
	}

	@Test
	void projectLabourMethodOverridesTheShopDefault() {
		var linearFoot = Project.builder().name("Cuisine").labourMethod(LabourMethod.LINEAR_FOOT).build();
		var hourly = Project.builder().name("Cuisine").labourMethod(LabourMethod.HOURLY)
				.labourHours(new BigDecimal("12")).build();

		assertThat(QuoteCalculator.calculate(SHOP, linearFoot, CLIENT, OUTPUT).getBreakdown().getLabour())
				.isEqualByComparingTo("1500.00");
		assertThat(QuoteCalculator.calculate(SHOP, hourly, CLIENT, OUTPUT).getBreakdown().getLabour())
				.isEqualByComparingTo("780.00");
	}
}
