package dev.daniellavoie.opencabinet.shop.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Tax {
	String name;
	BigDecimal ratePercent;
}
