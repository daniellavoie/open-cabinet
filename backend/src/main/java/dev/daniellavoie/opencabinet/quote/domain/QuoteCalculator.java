package dev.daniellavoie.opencabinet.quote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.cutlist.domain.model.ProjectOutput;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.quote.domain.model.DesignSnapshot;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteBreakdown;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteContent;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLine;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteLineKind;
import dev.daniellavoie.opencabinet.quote.domain.model.TaxLine;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import dev.daniellavoie.opencabinet.shop.domain.model.LabourSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;

/**
 * Prices a Project: materials from the Bill of Materials plus labour by the Project's Labour
 * Method, marked up, shown to the Client as one Cabinets line; Manual Lines are added as entered,
 * then the Shop's taxes apply to the subtotal.
 */
final class QuoteCalculator {
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private QuoteCalculator() {
	}

	static QuoteContent calculate(Shop shop, Project project, Client client, ProjectOutput output) {
		var settings = shop.getSettings();
		var operations = output.getOperations();
		var method = Optional.ofNullable(project.getLabourMethod()).orElse(settings.getLabour().getDefaultMethod());
		var hours = Optional.ofNullable(project.getLabourHours()).orElse(BigDecimal.ZERO);

		var materials = money(output.getMaterialsTotal());
		var labour = money(labour(method, settings.getLabour(), output, hours));
		var markupPercent = settings.getMarkupPercent();
		var markup = money(materials.add(labour).multiply(markupPercent).divide(HUNDRED));
		var cabinetsAmount = materials.add(labour).add(markup);

		var lines = new ArrayList<QuoteLine>();
		if (operations.getCabinets() > 0) {
			lines.add(QuoteLine.builder()
					.kind(QuoteLineKind.CABINETS)
					.description("Armoires sur mesure : " + operations.getCabinets() + " caisson(s), "
							+ decimal(operations.getLinearFeet()) + " pi linéaire(s)")
					.amount(cabinetsAmount)
					.build());
		}
		Optional.ofNullable(project.getManualLines()).orElse(List.of()).forEach(line -> lines.add(QuoteLine.builder()
				.kind(QuoteLineKind.MANUAL)
				.description(line.getDescription())
				.amount(money(line.getAmount()))
				.build()));

		var subtotal = lines.stream().map(QuoteLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		var taxes = Optional.ofNullable(settings.getTaxes()).orElse(List.of()).stream()
				.map(tax -> TaxLine.builder()
						.name(tax.getName())
						.ratePercent(tax.getRatePercent())
						.amount(money(subtotal.multiply(tax.getRatePercent()).divide(HUNDRED)))
						.build())
				.toList();
		var total = taxes.stream().map(TaxLine::getAmount).reduce(subtotal, BigDecimal::add);

		return QuoteContent.builder()
				.shopName(shop.getName())
				.clientName(client.getName())
				.clientEmail(client.getEmail())
				.clientPhone(client.getPhone())
				.clientAddress(client.getAddress())
				.projectName(project.getName())
				.currency(shop.getCurrency())
				.cabinetCount(operations.getCabinets())
				.lines(List.copyOf(lines))
				.subtotal(subtotal)
				.taxes(taxes)
				.total(total)
				.breakdown(QuoteBreakdown.builder()
						.materials(materials)
						.labourMethod(method)
						.labour(labour)
						.markupPercent(markupPercent)
						.markup(markup)
						.sheets(operations.getSheets())
						.edgeBandingMeters(operations.getEdgeBandingMeters())
						.hardwareItems(operations.getHardwareItems())
						.linearFeet(operations.getLinearFeet())
						.labourHours(hours)
						.build())
				.designSnapshot(DesignSnapshot.builder()
						.design(project.getDesign())
						.toeKickHeightMm(settings.getConstructionStandard().getToeKickHeightMm())
						.frontGapMm(settings.getConstructionStandard().getDoorGapMm())
						.units(shop.getUnits())
						.build())
				.build();
	}

	static BigDecimal labour(LabourMethod method, LabourSettings rates, ProjectOutput output, BigDecimal hours) {
		var operations = output.getOperations();

		return switch (method) {
		case OPERATION -> rates.getRatePerSheet().multiply(BigDecimal.valueOf(operations.getSheets()))
				.add(rates.getRatePerEdgeBandingMeter().multiply(operations.getEdgeBandingMeters()))
				.add(rates.getRatePerCabinet().multiply(BigDecimal.valueOf(operations.getCabinets())))
				.add(rates.getRatePerHardwareItem().multiply(BigDecimal.valueOf(operations.getHardwareItems())));
		case LINEAR_FOOT -> rates.getRatePerLinearFoot().multiply(operations.getLinearFeet());
		case HOURLY -> rates.getHourlyRate().multiply(hours);
		};
	}

	/**
	 * Quotes are written in French (Canada), where the decimal separator is a comma.
	 */
	static String decimal(BigDecimal value) {
		var format = NumberFormat.getNumberInstance(Locale.CANADA_FRENCH);
		format.setMaximumFractionDigits(2);
		return format.format(value);
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}
}
