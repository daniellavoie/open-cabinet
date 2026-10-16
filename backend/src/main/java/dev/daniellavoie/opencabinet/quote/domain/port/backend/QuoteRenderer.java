package dev.daniellavoie.opencabinet.quote.domain.port.backend;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;

public interface QuoteRenderer {
	/**
	 * Renders the Client-facing PDF of a Quote. Never includes the breakdown.
	 */
	byte[] pdf(Quote quote);
}
