package dev.daniellavoie.opencabinet.quote.domain.port.api;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;

public interface QuoteDomain {
	List<Quote> findByProject(UUID projectId, Principal principal);

	Quote findById(UUID quoteId, Principal principal);

	/**
	 * The Shop's DRAFT Quotes. Drafts keep the prices they were created with, so after a change to
	 * the Shop's settings or catalog they are worth re-creating.
	 */
	List<Quote> findDrafts(Principal principal);

	/**
	 * Prices the Project as it is now into a new DRAFT Quote.
	 */
	Quote create(UUID projectId, Principal principal);

	/**
	 * Prices the Project as it is now into the next DRAFT revision of a sent Quote's number.
	 */
	Quote revise(UUID quoteId, Principal principal);

	/**
	 * Freezes a DRAFT Quote: from then on it never changes. Earlier sent revisions become
	 * superseded.
	 */
	Quote send(UUID quoteId, Principal principal);

	Quote updateStatus(UUID quoteId, QuoteStatus status, Principal principal);

	void delete(UUID quoteId, Principal principal);

	byte[] pdf(UUID quoteId, Principal principal);
}
