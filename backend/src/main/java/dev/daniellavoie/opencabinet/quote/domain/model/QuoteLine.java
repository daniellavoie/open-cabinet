package dev.daniellavoie.opencabinet.quote.domain.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A line the Client sees on the Quote.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class QuoteLine {
	QuoteLineKind kind;
	String description;
	BigDecimal amount;
}
