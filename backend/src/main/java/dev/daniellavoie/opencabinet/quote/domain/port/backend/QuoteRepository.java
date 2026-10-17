package dev.daniellavoie.opencabinet.quote.domain.port.backend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;

public interface QuoteRepository {
	List<Quote> findByProject(UUID projectId, UUID shopId);

	Quote findById(UUID quoteId, UUID shopId);

	Quote create(Quote quote, UUID shopId);

	Quote update(Quote quote, UUID shopId);

	Optional<Quote> findLatestRevision(UUID shopId, int number);

	/**
	 * Marks the sent revisions before this one as superseded.
	 */
	void supersedeEarlierRevisions(Quote quote, UUID shopId);

	void delete(UUID quoteId, UUID shopId);
}
