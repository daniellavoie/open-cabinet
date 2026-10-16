package dev.daniellavoie.opencabinet.quote.domain.model;

/**
 * DRAFT Quotes can be deleted; once SENT a Quote is frozen and can only be ACCEPTED or DECLINED.
 */
public enum QuoteStatus {
	DRAFT, SENT, ACCEPTED, DECLINED
}
