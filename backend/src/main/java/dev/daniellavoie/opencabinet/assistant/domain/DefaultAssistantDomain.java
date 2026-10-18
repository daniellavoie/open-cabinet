package dev.daniellavoie.opencabinet.assistant.domain;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantUsage;
import dev.daniellavoie.opencabinet.assistant.domain.model.BudgetSpentException;
import dev.daniellavoie.opencabinet.assistant.domain.model.CheckReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ModelMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.model.UpdateAssistantSettingsRequest;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.AssistantDomain;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantRepository;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * The Design Assistant's gate (ADR 0004): nothing reaches the model until an Owner opts the Shop
 * in, and every call is refused once the month's budget is spent. Months are calendar months, in
 * the server's time.
 */
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultAssistantDomain implements AssistantDomain {
	static final BigDecimal MAX_MONTHLY_BUDGET_USD = new BigDecimal("10000");

	MemberDomain memberDomain;
	MemberRepository memberRepository;
	AssistantRepository repository;
	AssistantModel model;

	@Override
	public AssistantStatus status(Principal principal) {
		return status(memberDomain.current(principal));
	}

	@Override
	public AssistantStatus updateSettings(UpdateAssistantSettingsRequest request, Principal principal) {
		var owner = memberDomain.currentOwner(principal);
		var budget = Optional.ofNullable(request.getMonthlyBudgetUsd())
				.filter(value -> value.signum() > 0 && value.compareTo(MAX_MONTHLY_BUDGET_USD) <= 0)
				.orElseThrow(() -> new IllegalArgumentException(
						"a monthly budget above 0 and up to " + MAX_MONTHLY_BUDGET_USD + " USD is required"));
		if (request.isEnabled() && !model.isConfigured()) {
			throw new IllegalStateException("the server has no model provider key, so the assistant cannot be turned on");
		}

		var current = repository.findSettings(owner.getShopId());
		var turnedOn = request.isEnabled() && !current.map(AssistantSettings::isEnabled).orElse(false);
		repository.saveSettings(AssistantSettings.builder()
				.shopId(owner.getShopId())
				.enabled(request.isEnabled())
				.monthlyBudgetUsd(budget)
				.enabledBy(turnedOn ? owner.getId() : current.map(AssistantSettings::getEnabledBy).orElse(null))
				.enabledTimestamp(turnedOn ? LocalDateTime.now()
						: current.map(AssistantSettings::getEnabledTimestamp).orElse(null))
				.build());
		return status(owner);
	}

	@Override
	public CheckReply check(Principal principal) {
		memberDomain.currentOwner(principal);
		var reply = complete(ModelRequest.builder()
				.tier(ModelTier.SORTER)
				.purpose("CHECK")
				.instructions("You are checking that a connection works. Reply with the single word: ready.")
				.messages(List.of(new ModelMessage(Speaker.MEMBER, "Are you there?")))
				.maxTokens(16)
				.build(), principal);
		return CheckReply.builder()
				.model(reply.getModel())
				.text(reply.getText())
				.costUsd(reply.getCostUsd())
				.build();
	}

	@Override
	public ModelReply complete(ModelRequest request, Principal principal) {
		var member = memberDomain.current(principal);
		return complete(request, member.getShopId(), member.getId());
	}

	@Override
	public boolean isAvailable(UUID shopId) {
		return model.isConfigured() && repository.findSettings(shopId).map(AssistantSettings::isEnabled).orElse(false);
	}

	@Override
	public ModelReply complete(ModelRequest request, UUID shopId, UUID memberId) {
		var settings = repository.findSettings(shopId)
				.filter(AssistantSettings::isEnabled)
				.orElseThrow(() -> new IllegalStateException("the design assistant is off for this shop"));
		if (!model.isConfigured()) {
			throw new IllegalStateException("the server has no model provider key");
		}
		if (spentThisMonth(shopId).compareTo(settings.getMonthlyBudgetUsd()) >= 0) {
			throw new BudgetSpentException();
		}

		var reply = model.complete(request);
		repository.recordUsage(AssistantUsage.builder()
				.id(UUID.randomUUID())
				.shopId(shopId)
				.memberId(memberId)
				.purpose(request.getPurpose())
				.model(reply.getModel())
				.inputTokens(reply.getInputTokens())
				.outputTokens(reply.getOutputTokens())
				.costUsd(reply.getCostUsd())
				.creationTimestamp(LocalDateTime.now())
				.build());
		return reply;
	}

	private AssistantStatus status(Member member) {
		var settings = repository.findSettings(member.getShopId());
		var enabledBy = settings.map(AssistantSettings::getEnabledBy);
		return AssistantStatus.builder()
				.configured(model.isConfigured())
				.enabled(settings.map(AssistantSettings::isEnabled).orElse(false))
				.monthlyBudgetUsd(settings.map(AssistantSettings::getMonthlyBudgetUsd).orElse(null))
				.spentThisMonthUsd(spentThisMonth(member.getShopId()))
				.enabledByName(enabledBy.map(id -> memberRepository.findById(id, member.getShopId()).getName())
						.orElse(null))
				.enabledTimestamp(settings.map(AssistantSettings::getEnabledTimestamp).orElse(null))
				.builderModel(model.modelFor(ModelTier.BUILDER))
				.sorterModel(model.modelFor(ModelTier.SORTER))
				.build();
	}

	private BigDecimal spentThisMonth(UUID shopId) {
		return repository.spentSince(shopId, LocalDate.now().withDayOfMonth(1).atStartOfDay());
	}
}
