package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The quantities the Labour Methods price.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class OperationCounts {
	int sheets;
	BigDecimal edgeBandingMeters;
	int cabinets;
	int hardwareItems;
	BigDecimal linearFeet;
}
