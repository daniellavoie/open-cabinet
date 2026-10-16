package dev.daniellavoie.opencabinet.shop.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Rates for every Labour Method. The Shop picks a default method; each Project may use another.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class LabourSettings {
	LabourMethod defaultMethod;

	// OPERATION
	BigDecimal ratePerSheet;
	BigDecimal ratePerEdgeBandingMeter;
	BigDecimal ratePerCabinet;
	BigDecimal ratePerHardwareItem;

	// LINEAR_FOOT
	BigDecimal ratePerLinearFoot;

	// HOURLY
	BigDecimal hourlyRate;
}
