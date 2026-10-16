package dev.daniellavoie.opencabinet.quote.domain.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Everything a Quote says, captured when it is created so later changes to the design, the catalog
 * or the Shop's settings never alter it.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class QuoteContent {
	String shopName;
	String clientName;
	String clientEmail;
	String clientPhone;
	String clientAddress;
	String projectName;
	String currency;
	int cabinetCount;
	List<QuoteLine> lines;
	BigDecimal subtotal;
	List<TaxLine> taxes;
	BigDecimal total;
	QuoteBreakdown breakdown;
}
