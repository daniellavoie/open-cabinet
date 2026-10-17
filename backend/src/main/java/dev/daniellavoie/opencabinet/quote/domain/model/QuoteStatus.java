package dev.daniellavoie.opencabinet.quote.domain.model;

/**
 * DRAFT Quotes can be deleted; once SENT a Quote is frozen and can only be ACCEPTED or DECLINED, or
 * SUPERSEDED when a later Quote Revision of it is sent.
 */
public enum QuoteStatus {
	DRAFT, SENT, ACCEPTED, DECLINED, SUPERSEDED
}
