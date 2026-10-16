package dev.daniellavoie.opencabinet.quote.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TaxLine {
	String name;
	BigDecimal ratePercent;
	BigDecimal amount;
}
