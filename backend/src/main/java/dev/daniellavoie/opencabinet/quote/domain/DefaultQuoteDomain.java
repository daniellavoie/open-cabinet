package dev.daniellavoie.opencabinet.quote.domain;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.port.api.CutListDomain;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.quote.domain.model.Quote;
import dev.daniellavoie.opencabinet.quote.domain.model.QuoteStatus;
import dev.daniellavoie.opencabinet.quote.domain.port.api.QuoteDomain;
import dev.daniellavoie.opencabinet.quote.domain.port.backend.QuoteRenderer;
import dev.daniellavoie.opencabinet.quote.domain.port.backend.QuoteRepository;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultQuoteDomain implements QuoteDomain {
	QuoteRepository repository;
	QuoteRenderer renderer;
	MemberDomain memberDomain;
	ProjectDomain projectDomain;
	ClientDomain clientDomain;
	CatalogDomain catalogDomain;
	CutListDomain cutListDomain;
	ShopRepository shopRepository;

	@Override
	public List<Quote> findByProject(UUID projectId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		projectDomain.findById(projectId, shopId);

		return repository.findByProject(projectId, shopId);
	}

	@Override
	public Quote findById(UUID quoteId, Principal principal) {
		return repository.findById(quoteId, memberDomain.current(principal).getShopId());
	}

	@Override
	public List<Quote> findDrafts(Principal principal) {
		return repository.findDrafts(memberDomain.current(principal).getShopId());
	}

	@Override
	public Quote create(UUID projectId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var draft = draft(projectId, shopId);

		return repository.create(draft.toBuilder().number(shopRepository.nextQuoteNumber(shopId)).build(), shopId);
	}

	@Override
	public Quote revise(UUID quoteId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var quote = repository.findById(quoteId, shopId);
		var latest = repository.findLatestRevision(shopId, quote.getNumber()).orElseThrow();

		if (latest.getRevision() != quote.getRevision()) {
			throw new IllegalStateException("only the latest revision of a quote can be revised");
		}
		if (quote.getStatus() == QuoteStatus.DRAFT) {
			throw new IllegalStateException("a draft quote is changed by recreating it, not revised");
		}

		return repository.create(draft(quote.getProjectId(), shopId).toBuilder()
				.number(quote.getNumber())
				.revision(quote.getRevision() + 1)
				.build(), shopId);
	}

	/**
	 * Prices the Project as it is now, without a number yet.
	 */
	private Quote draft(UUID projectId, UUID shopId) {
		var shop = shopRepository.findById(shopId);
		var project = projectDomain.findById(projectId, shopId);
		var output = cutListDomain.output(project, shop.getSettings().getConstructionStandard(),
				catalogDomain.findByShop(shopId));

		if (output.getOperations().getCabinets() == 0
				&& Optional.ofNullable(project.getManualLines()).orElse(List.of()).isEmpty()) {
			throw new IllegalStateException("add cabinets or manual lines before quoting");
		}

		return Quote.builder()
				.id(UUID.randomUUID())
				.projectId(projectId)
				.status(QuoteStatus.DRAFT)
				.content(QuoteCalculator.calculate(shop, project, clientDomain.findById(project.getClientId(), shopId),
						output))
				.creationTimestamp(LocalDateTime.now())
				.build();
	}

	@Override
	public Quote send(UUID quoteId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var quote = repository.findById(quoteId, shopId);

		if (quote.getStatus() != QuoteStatus.DRAFT) {
			throw new IllegalStateException("only a draft quote can be sent");
		}

		var sent = repository.update(quote.toBuilder()
				.status(QuoteStatus.SENT)
				.sentTimestamp(LocalDateTime.now())
				.build(), shopId);
		repository.supersedeEarlierRevisions(sent, shopId);
		return sent;
	}

	@Override
	public Quote updateStatus(UUID quoteId, QuoteStatus status, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var quote = repository.findById(quoteId, shopId);

		if (status != QuoteStatus.ACCEPTED && status != QuoteStatus.DECLINED) {
			throw new IllegalArgumentException("a quote can only be marked accepted or declined");
		}
		if (quote.getStatus() != QuoteStatus.SENT) {
			throw new IllegalStateException("only a sent quote can be accepted or declined");
		}

		return repository.update(quote.toBuilder().status(status).build(), shopId);
	}

	@Override
	public void delete(UUID quoteId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();

		if (repository.findById(quoteId, shopId).getStatus() != QuoteStatus.DRAFT) {
			throw new IllegalStateException("a sent quote is kept as it was sent");
		}

		repository.delete(quoteId, shopId);
	}

	@Override
	public byte[] pdf(UUID quoteId, Principal principal) {
		return renderer.pdf(findById(quoteId, principal));
	}
}
