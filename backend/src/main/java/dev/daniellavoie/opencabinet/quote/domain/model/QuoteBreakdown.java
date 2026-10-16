package dev.daniellavoie.opencabinet.quote.domain.model;

import java.math.BigDecimal;

import dev.daniellavoie.opencabinet.shop.domain.model.LabourMethod;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * How the Shop arrived at the Cabinets line. Never shown to the Client.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class QuoteBreakdown {
	BigDecimal materials;
	LabourMethod labourMethod;
	BigDecimal labour;
	BigDecimal markupPercent;
	BigDecimal markup;
	int sheets;
	BigDecimal edgeBandingMeters;
	int hardwareItems;
	BigDecimal linearFeet;
	BigDecimal labourHours;
}
