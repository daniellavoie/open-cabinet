package dev.daniellavoie.opencabinet.quote.domain.port.backend;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuotePictures;

public interface QuoteRenderer {
	/**
	 * Renders the Client-facing PDF of a Quote with pictures of the design (none for Quotes created
	 * before Quotes carried pictures). Never includes the breakdown.
	 */
	byte[] pdf(Quote quote, QuotePictures pictures);
}
