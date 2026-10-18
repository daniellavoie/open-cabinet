package dev.daniellavoie.opencabinet.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantUsage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.model.UpdateAssistantSettingsRequest;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantRepository;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.Role;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;

class DefaultAssistantDomainTest {
	private final MemberDomain memberDomain = mock(MemberDomain.class);
	private final AssistantRepository repository = mock(AssistantRepository.class);
	private final AssistantModel model = mock(AssistantModel.class);
	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final DefaultAssistantDomain domain = new DefaultAssistantDomain(memberDomain, memberRepository, repository,
			model);
	private final Principal principal = () -> "owner";
	private final Member owner = Member.builder().id(UUID.randomUUID()).shopId(UUID.randomUUID()).name("Marie")
			.role(Role.OWNER).build();
	private final ModelRequest request = ModelRequest.builder().tier(ModelTier.BUILDER).purpose("PROPOSAL")
			.instructions("Help.").messages(List.of()).maxTokens(100).build();

	@BeforeEach
	void signIn() {
		when(memberDomain.current(principal)).thenReturn(owner);
		when(memberDomain.currentOwner(principal)).thenReturn(owner);
		when(memberRepository.findById(any(), eq(owner.getShopId()))).thenReturn(owner);
		when(model.isConfigured()).thenReturn(true);
		when(repository.spentSince(eq(owner.getShopId()), any())).thenReturn(BigDecimal.ZERO);
	}

	@Test
	void nothingIsSentUntilTheShopOptsIn() {
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> domain.complete(request, principal)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("off");
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.of(settings(false, "20")));
		assertThatThrownBy(() -> domain.complete(request, principal)).isInstanceOf(IllegalStateException.class);
		verify(model, never()).complete(any());
	}

	@Test
	void stopsOnceTheMonthsBudgetIsSpent() {
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.of(settings(true, "20")));
		when(repository.spentSince(eq(owner.getShopId()), any())).thenReturn(new BigDecimal("20.000001"));

		assertThatThrownBy(() -> domain.complete(request, principal)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("budget");
		verify(model, never()).complete(any());
	}

	@Test
	void recordsWhatEachCallCostsSinceTheStartOfTheMonth() {
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.of(settings(true, "20")));
		when(model.complete(request)).thenReturn(ModelReply.builder().text("Voici.").model("sonnet").inputTokens(1200)
				.outputTokens(300).costUsd(new BigDecimal("0.0081")).build());

		assertThat(domain.complete(request, principal).getText()).isEqualTo("Voici.");

		var usage = ArgumentCaptor.forClass(AssistantUsage.class);
		verify(repository).recordUsage(usage.capture());
		assertThat(usage.getValue().getShopId()).isEqualTo(owner.getShopId());
		assertThat(usage.getValue().getMemberId()).isEqualTo(owner.getId());
		assertThat(usage.getValue().getPurpose()).isEqualTo("PROPOSAL");
		assertThat(usage.getValue().getCostUsd()).isEqualByComparingTo("0.0081");
		var since = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(repository).spentSince(eq(owner.getShopId()), since.capture());
		assertThat(since.getValue().getDayOfMonth()).isEqualTo(1);
		assertThat(since.getValue().toLocalTime().toSecondOfDay()).isZero();
	}

	@Test
	void anOwnerTurnsItOnWithABudgetOnlyWhenTheServerHasAKey() {
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> domain.updateSettings(update(true, "0"), principal))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> domain.updateSettings(update(true, "10001"), principal))
				.isInstanceOf(IllegalArgumentException.class);
		when(model.isConfigured()).thenReturn(false);
		assertThatThrownBy(() -> domain.updateSettings(update(true, "20"), principal))
				.isInstanceOf(IllegalStateException.class);
		when(model.isConfigured()).thenReturn(true);

		domain.updateSettings(update(true, "20"), principal);

		var saved = ArgumentCaptor.forClass(AssistantSettings.class);
		verify(repository).saveSettings(saved.capture());
		assertThat(saved.getValue().isEnabled()).isTrue();
		assertThat(saved.getValue().getEnabledBy()).isEqualTo(owner.getId());
		assertThat(saved.getValue().getEnabledTimestamp()).isNotNull();
	}

	@Test
	void changingTheBudgetKeepsWhoTurnedItOn() {
		var earlier = settings(true, "20").toBuilder().enabledBy(UUID.randomUUID())
				.enabledTimestamp(LocalDateTime.of(2026, 9, 1, 8, 0)).build();
		when(repository.findSettings(owner.getShopId())).thenReturn(Optional.of(earlier));

		domain.updateSettings(update(true, "50"), principal);

		var saved = ArgumentCaptor.forClass(AssistantSettings.class);
		verify(repository).saveSettings(saved.capture());
		assertThat(saved.getValue().getMonthlyBudgetUsd()).isEqualByComparingTo("50");
		assertThat(saved.getValue().getEnabledBy()).isEqualTo(earlier.getEnabledBy());
		assertThat(saved.getValue().getEnabledTimestamp()).isEqualTo(earlier.getEnabledTimestamp());
	}

	private AssistantSettings settings(boolean enabled, String budget) {
		return AssistantSettings.builder().shopId(owner.getShopId()).enabled(enabled)
				.monthlyBudgetUsd(new BigDecimal(budget)).build();
	}

	private static UpdateAssistantSettingsRequest update(boolean enabled, String budget) {
		return UpdateAssistantSettingsRequest.builder().enabled(enabled).monthlyBudgetUsd(new BigDecimal(budget))
				.build();
	}
}
