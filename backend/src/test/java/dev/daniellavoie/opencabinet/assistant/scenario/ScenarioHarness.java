package dev.daniellavoie.opencabinet.assistant.scenario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.assistant.domain.ConversationProgress;
import dev.daniellavoie.opencabinet.assistant.domain.DefaultConversationDomain;
import dev.daniellavoie.opencabinet.assistant.domain.DesignIds;
import dev.daniellavoie.opencabinet.assistant.domain.InMemoryAttachments;
import dev.daniellavoie.opencabinet.assistant.domain.InMemoryConversations;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.CheckReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UpdateAssistantSettingsRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.AssistantDomain;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;
import dev.daniellavoie.opencabinet.assistant.module.adapter.pdf.OpenPdfInspector;
import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.Role;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.ShopSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;

/**
 * Runs a scenario through the Design Assistant's real Conversation domain, the Project and the
 * Shop in memory, with whichever model is given: the real one, recording, or a replay.
 */
final class ScenarioHarness {
	static final JsonMapper JSON = JsonConfiguration.configure(JsonMapper.builder()).build();
	static final String SCENARIOS = "assistant-scenarios/";

	/**
	 * What a run produced, and what it cost.
	 */
	record Run(ConversationView view, List<ScenarioChecks.Check> checks, int rounds, long inputTokens, long outputTokens,
			BigDecimal costUsd, Duration duration) {
		boolean passed() {
			return checks.stream().allMatch(ScenarioChecks.Check::passed);
		}
	}

	private ScenarioHarness() {
	}

	static Scenario scenario(String name) {
		return read(SCENARIOS + name + ".json", new TypeReference<Scenario>() {
		});
	}

	static List<CabinetTemplate> templates() {
		return read("cabinet-templates.json", new TypeReference<List<CabinetTemplate>>() {
		});
	}

	static Run run(Scenario scenario, AssistantModel model) {
		var shopId = UUID.randomUUID();
		var member = Member.builder().id(UUID.randomUUID()).shopId(shopId).name("Marie").role(Role.OWNER).build();
		Principal principal = () -> "marie";
		var project = Project.builder()
				.id(UUID.randomUUID())
				.clientId(UUID.randomUUID())
				.name("Cuisine Tremblay")
				.design(scenario.design())
				.updateTimestamp(LocalDateTime.now())
				.build();
		var templates = templates();

		var memberDomain = mock(MemberDomain.class);
		when(memberDomain.current(principal)).thenReturn(member);
		var memberRepository = mock(MemberRepository.class);
		when(memberRepository.findById(any(), eq(shopId))).thenReturn(member);
		var projectDomain = mock(ProjectDomain.class);
		when(projectDomain.findById(project.getId(), shopId)).thenReturn(project);
		when(projectDomain.templates(any(Project.class))).thenReturn(templates);
		var clientDomain = mock(ClientDomain.class);
		when(clientDomain.findById(project.getClientId(), shopId)).thenReturn(Client.builder().name("Jean Tremblay").build());
		var catalogDomain = mock(CatalogDomain.class);
		when(catalogDomain.findByShop(shopId)).thenReturn(List.of(
				Material.builder().id(UUID.randomUUID()).kind(MaterialKind.FRONT).name("Shaker blanc").build()));
		var shopRepository = mock(ShopRepository.class);
		when(shopRepository.findById(shopId)).thenReturn(Shop.builder()
				.units(scenario.units() == null ? Units.IMPERIAL : scenario.units())
				.settings(ShopSettings.builder()
						.constructionStandard(ConstructionStandard.builder().doorGapMm(3).toeKickHeightMm(101.6).build())
						.build())
				.build());

		var gate = new MeteredGate(model);
		var domain = new DefaultConversationDomain(memberDomain, memberRepository, projectDomain, clientDomain, catalogDomain,
				shopRepository, gate, new InMemoryConversations(), new ConversationProgress(), new InMemoryAttachments(),
				new OpenPdfInspector(), new SequentialDesignIds(), Runnable::run, JSON);

		var started = System.nanoTime();
		for (var message : scenario.messages()) {
			var files = message.attachments().stream()
					.map(name -> new UploadedFile(name, bytes(SCENARIOS + name)))
					.toList();
			domain.send(project.getId(), SendMessageRequest.builder().text(message.text()).language(scenario.language()).build(),
					files, principal);
		}
		var view = domain.view(project.getId(), 0, principal);
		return new Run(view, ScenarioChecks.check(scenario, view, templates), gate.rounds, gate.inputTokens, gate.outputTokens,
				gate.costUsd, Duration.ofNanos(System.nanoTime() - started));
	}

	private static byte[] bytes(String path) {
		try (var input = new ClassPathResource(path).getInputStream()) {
			return input.readAllBytes();
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private static <T> T read(String path, TypeReference<T> type) {
		try (var input = new ClassPathResource(path).getInputStream()) {
			return JSON.readValue(input, type);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Ids numbered in order, so a replay creates the ids its recorded tool calls name.
	 */
	static final class SequentialDesignIds extends DesignIds {
		private final Map<String, Integer> counts = new HashMap<>();

		@Override
		public String next(String prefix) {
			return prefix + "-" + counts.merge(prefix, 1, Integer::sum);
		}
	}

	/**
	 * The assistant's gate for scenarios: always open, and counting what each round costs.
	 */
	private static final class MeteredGate implements AssistantDomain {
		private final AssistantModel model;
		private int rounds;
		private long inputTokens;
		private long outputTokens;
		private BigDecimal costUsd = BigDecimal.ZERO;

		MeteredGate(AssistantModel model) {
			this.model = model;
		}

		@Override
		public ModelReply complete(ModelRequest request, UUID shopId, UUID memberId) {
			var reply = model.complete(request);
			rounds++;
			inputTokens += reply.getInputTokens();
			outputTokens += reply.getOutputTokens();
			costUsd = costUsd.add(reply.getCostUsd() == null ? BigDecimal.ZERO : reply.getCostUsd());
			return reply;
		}

		@Override
		public boolean isAvailable(UUID shopId) {
			return true;
		}

		@Override
		public ModelReply complete(ModelRequest request, Principal principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AssistantStatus status(Principal principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AssistantStatus updateSettings(UpdateAssistantSettingsRequest request, Principal principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CheckReply check(Principal principal) {
			throw new UnsupportedOperationException();
		}
	}
}
