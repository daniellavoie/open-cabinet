package dev.daniellavoie.opencabinet.quote.module.adapter.database;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import dev.daniellavoie.opencabinet.quote.domain.port.backend.QuoteRepository;
import dev.daniellavoie.opencabinet.quote.module.adapter.database.entity.QuoteEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuoteDatabaseRepository implements QuoteRepository {
	QuoteJpaRepository jpaRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Quote> findByProject(UUID projectId, UUID shopId) {
		return jpaRepository.findByProjectIdAndShopIdOrderByNumberDescRevisionDesc(projectId, shopId)
				.stream()
				.map(QuoteEntity::to)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Quote findById(UUID quoteId, UUID shopId) {
		return find(quoteId, shopId).to();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Quote> findDrafts(UUID shopId) {
		return jpaRepository.findByShopIdAndStatusOrderByNumberAsc(shopId, QuoteStatus.DRAFT)
				.stream()
				.map(QuoteEntity::to)
				.toList();
	}

	@Override
	@Transactional
	public Quote create(Quote quote, UUID shopId) {
		log.info("Creating quote {}/{} number {} with: { total = {}, currency = {} }", shopId, quote.getId(),
				quote.getNumber(), quote.getContent().getTotal(), quote.getContent().getCurrency());

		return jpaRepository.save(QuoteEntity.builder()
				.id(quote.getId())
				.shopId(shopId)
				.projectId(quote.getProjectId())
				.number(quote.getNumber())
				.revision(quote.getRevision())
				.status(quote.getStatus())
				.content(quote.getContent())
				.creationTimestamp(quote.getCreationTimestamp())
				.build())
				.to();
	}

	/**
	 * Only the status and sent time ever change; the content of a Quote is never rewritten.
	 */
	@Override
	@Transactional
	public Quote update(Quote quote, UUID shopId) {
		var entity = find(quote.getId(), shopId);
		if (entity.getStatus() != QuoteStatus.DRAFT && quote.getStatus() == QuoteStatus.DRAFT) {
			throw new IllegalStateException("a sent quote cannot return to draft");
		}

		return jpaRepository.save(entity.toBuilder()
				.status(quote.getStatus())
				.sentTimestamp(quote.getSentTimestamp())
				.build())
				.to();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Quote> findLatestRevision(UUID shopId, int number) {
		return jpaRepository.findFirstByShopIdAndNumberOrderByRevisionDesc(shopId, number).map(QuoteEntity::to);
	}

	@Override
	@Transactional
	public void supersedeEarlierRevisions(Quote quote, UUID shopId) {
		jpaRepository.supersede(shopId, quote.getNumber(), quote.getRevision(), QuoteStatus.SENT,
				QuoteStatus.SUPERSEDED);
	}

	@Override
	@Transactional
	public void delete(UUID quoteId, UUID shopId) {
		jpaRepository.delete(find(quoteId, shopId));
	}

	private QuoteEntity find(UUID quoteId, UUID shopId) {
		return jpaRepository.findByIdAndShopId(quoteId, shopId)
				.orElseThrow(() -> new NoSuchElementException("quote " + quoteId + " not found"));
	}
}
