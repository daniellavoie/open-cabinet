package dev.daniellavoie.opencabinet.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.assistant.domain.model.AnswerError;
import dev.daniellavoie.opencabinet.assistant.domain.model.AssistantSettings;
import dev.daniellavoie.opencabinet.assistant.domain.model.Attachment;
import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.model.Author;
import dev.daniellavoie.opencabinet.assistant.domain.model.BudgetSpentException;
import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProgressEvent;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.AssistantDomain;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantRepository;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AttachmentStore;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.ConversationRepository;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.model.Client;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.model.Role;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.ProjectLock;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;
import dev.daniellavoie.opencabinet.shop.domain.model.Shop;
import dev.daniellavoie.opencabinet.shop.domain.model.ShopSettings;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;

class DefaultConversationDomainTest {
	private static final JsonMapper JSON = JsonConfiguration.configure(JsonMapper.builder()).build();

	private final MemberDomain memberDomain = mock(MemberDomain.class);
	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final ProjectDomain projectDomain = mock(ProjectDomain.class);
	private final ClientDomain clientDomain = mock(ClientDomain.class);
	private final CatalogDomain catalogDomain = mock(CatalogDomain.class);
	private final ShopRepository shopRepository = mock(ShopRepository.class);
	private final AssistantDomain assistantDomain = mock(AssistantDomain.class);
	private final AssistantRepository assistantSettings = mock(AssistantRepository.class);
	private final InMemoryConversations repository = new InMemoryConversations();
	private final ConversationProgress progress = new ConversationProgress();
	private final InMemoryAttachments attachments = new InMemoryAttachments();
	private final DefaultConversationDomain domain = new DefaultConversationDomain(memberDomain, memberRepository, projectDomain,
			clientDomain, catalogDomain, shopRepository, assistantDomain, repository, progress, attachments, pdf -> 2, new SequentialIds(), Runnable::run,
			JSON, assistantSettings);

	private final Principal principal = () -> "marie";
	private final Member member = Member.builder().id(UUID.randomUUID()).shopId(UUID.randomUUID()).name("Marie")
			.role(Role.DESIGNER).build();
	private final UUID projectId = UUID.randomUUID();
	private final List<ModelRequest> requests = new ArrayList<>();
	private final Deque<Supplier<ModelReply>> replies = new ArrayDeque<>();
	private Project project;

	@BeforeEach
	void setUp() {
		project = Project.builder()
				.id(projectId)
				.clientId(UUID.randomUUID())
				.name("Cuisine")
				.design(Design.builder()
						.walls(List.of(Wall.builder().id("w1").lengthMm(3000).heightMm(2400).angleToNextDegrees(90).build()))
						.cabinets(List.of())
						.build())
				.updateTimestamp(LocalDateTime.of(2026, 10, 1, 9, 0))
				.build();
		when(memberDomain.current(principal)).thenReturn(member);
		when(memberRepository.findById(any(), eq(member.getShopId()))).thenReturn(member);
		when(projectDomain.findById(projectId, member.getShopId())).thenAnswer(invocation -> project);
		when(projectDomain.templates(any(Project.class))).thenReturn(templates());
		when(projectDomain.lock(projectId, principal)).thenReturn(ProjectLock.builder().mine(true).build());
		when(projectDomain.update(eq(projectId), any(UpdateProjectRequest.class), eq(principal))).thenAnswer(invocation -> {
			UpdateProjectRequest request = invocation.getArgument(1);
			project = project.toBuilder().design(request.getDesign()).updateTimestamp(project.getUpdateTimestamp().plusMinutes(1))
					.build();
			return project;
		});
		when(clientDomain.findById(any(UUID.class), eq(member.getShopId()))).thenReturn(Client.builder().name("Jean").build());
		when(catalogDomain.findByShop(member.getShopId())).thenReturn(List.of());
		when(shopRepository.findById(member.getShopId())).thenReturn(Shop.builder().units(Units.IMPERIAL)
				.settings(ShopSettings.builder().markupPercent(BigDecimal.TEN)
						.constructionStandard(ConstructionStandard.builder().doorGapMm(3).toeKickHeightMm(101.6).build())
						.build())
				.build());
		when(assistantDomain.isAvailable(member.getShopId())).thenReturn(true);
		when(assistantDomain.complete(any(ModelRequest.class), eq(member.getShopId()), eq(member.getId()))).thenAnswer(invocation -> {
			requests.add(invocation.getArgument(0));
			return replies.removeFirst().get();
		});
	}

	@Test
	void buildsAProposalFromTheStepsThatPassTheRules() {
		replies.add(() -> calls(
				call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}"),
				call("2", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"widthMm\": 5000}")));
		replies.add(() -> calls(call("3", "note_proposal",
				"{\"summary\": \"Un caisson bas\", \"openQuestions\": [\"Quelle poignée ?\"], \"assumptions\": [\"Largeur par défaut\"]}")));
		replies.add(() -> text("Voici un caisson bas contre le mur."));

		domain.send(projectId, SendMessageRequest.builder().text("Ajoute un caisson bas").language("fr").build(), List.of(), principal);

		var view = domain.view(projectId, 0, principal);
		var proposal = view.getProposal();
		assertThat(view.isRunning()).isFalse();
		assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.OPEN);
		assertThat(proposal.getSteps()).extracting("tool").containsExactly("add_cabinet");
		assertThat(proposal.getDesign().getCabinets()).hasSize(1);
		assertThat(proposal.getBaseDesign()).isEqualTo(project.getDesign());
		assertThat(proposal.getSummary()).isEqualTo("Un caisson bas");
		assertThat(proposal.getOpenQuestions()).containsExactly("Quelle poignée ?");
		assertThat(proposal.getAssumptions()).containsExactly("Largeur par défaut");
		assertThat(view.getMessages()).extracting(ConversationMessage::getAuthor).containsExactly(Author.MEMBER, Author.ASSISTANT);
		assertThat(view.getMessages().get(0).getMemberName()).isEqualTo("Marie");
		assertThat(view.getMessages().get(1).getText()).isEqualTo("Voici un caisson bas contre le mur.");
		assertThat(view.getMessages().get(1).getProposalId()).isEqualTo(proposal.getId());
		assertThat(view.getProgress()).extracting(ProgressEvent::getKind).containsExactly(ProgressEvent.Kind.THINKING,
				ProgressEvent.Kind.STEP, ProgressEvent.Kind.REFUSED, ProgressEvent.Kind.THINKING, ProgressEvent.Kind.NOTED,
				ProgressEvent.Kind.THINKING);
		assertThat(view.getProgress().get(2).getViolations()).extracting("code").contains("WIDTH_OUT_OF_RANGE");

		// The model hears back about the refused step, and works from the project's design in French.
		var second = requests.get(1);
		assertThat(second.getMessages()).filteredOn(message -> message.speaker() == Speaker.TOOL)
				.extracting(message -> message.toolResult().content())
				.anySatisfy(content -> assertThat(content).contains("\"ok\":false", "WIDTH_OUT_OF_RANGE"));
		assertThat(requests.get(0).getInstructions()).contains("Answer in French", "BASE_DOOR", "Jean", "inches");
		assertThat(requests.get(0).getInstructions()).doesNotContain("price");
	}

	@Test
	void theNextRequestContinuesTheOpenProposal() {
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> text("Un caisson."));
		domain.send(projectId, SendMessageRequest.builder().text("Un caisson").build(), List.of(), principal);
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> text("Un deuxième."));

		domain.send(projectId, SendMessageRequest.builder().text("Un autre").build(), List.of(), principal);

		var proposal = domain.view(projectId, 0, principal).getProposal();
		assertThat(proposal.getSteps()).hasSize(2);
		assertThat(proposal.getDesign().getCabinets()).extracting(Cabinet::getOffsetMm).containsExactly(0.0, 457.2);
		assertThat(requests.get(2).getMessages()).extracting(ModelRequest.ModelMessage::text)
				.contains("Un caisson", "Un caisson.", "Un autre");
		assertThat(requests.get(2).getInstructions()).contains("open Design Proposal");
	}

	@Test
	void keepsTheStepsTakenWhenTheBudgetRunsOut() {
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> {
			throw new BudgetSpentException();
		});

		domain.send(projectId, SendMessageRequest.builder().text("Deux caissons").build(), List.of(), principal);

		var view = domain.view(projectId, 0, principal);
		assertThat(view.getProposal().getSteps()).hasSize(1);
		assertThat(view.getMessages().get(1).getError()).isEqualTo(AnswerError.BUDGET_SPENT);
		assertThat(view.isRunning()).isFalse();
	}

	@Test
	void sendsAttachmentsWithTheirMessage() {
		var png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3 };
		replies.add(() -> text("Je vois un mur de 10 pi, à confirmer."));

		domain.send(projectId, SendMessageRequest.builder().text("Voici la cuisine").build(),
				List.of(new UploadedFile("photos/cuisine.png", png)), principal);

		var member = requests.get(0).getMessages().get(0);
		assertThat(member.attachments()).hasSize(1);
		assertThat(member.attachments().get(0).mediaType()).isEqualTo("image/png");
		assertThat(member.attachments().get(0).bytes()).isEqualTo(png);
		assertThat(requests.get(0).getInstructions()).contains("photo or an undimensioned sketch is always an");
		var listed = domain.view(projectId, 0, principal).getMessages().get(0).getAttachments();
		assertThat(listed).extracting(Attachment::getFileName, Attachment::getMediaType, Attachment::getSizeBytes)
				.containsExactly(org.assertj.core.groups.Tuple.tuple("cuisine.png", "image/png", (long) png.length));
		assertThat(domain.attachment(projectId, listed.get(0).getId(), principal).bytes()).isEqualTo(png);
	}

	@Test
	void refusesFilesTheModelCannotTake() {
		assertThatThrownBy(() -> domain.send(projectId, SendMessageRequest.builder().text("Voici").build(),
				List.of(new UploadedFile("notes.txt", "hello".getBytes())), principal))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("notes.txt");
		assertThat(domain.view(projectId, 0, principal).getMessages()).isEmpty();
	}

	@Test
	void anEmptyOrCutShortAnswerIsAnError() {
		replies.add(() -> ModelReply.builder().model("fake").costUsd(BigDecimal.ZERO).build());
		domain.send(projectId, SendMessageRequest.builder().text("Réorganise la cuisine").build(), List.of(), principal);
		replies.add(() -> ModelReply.builder().text("Je commence par").truncated(true).model("fake").costUsd(BigDecimal.ZERO).build());
		domain.send(projectId, SendMessageRequest.builder().text("Encore").build(), List.of(), principal);

		var answers = domain.view(projectId, 0, principal).getMessages().stream()
				.filter(message -> message.getAuthor() == Author.ASSISTANT).toList();
		assertThat(answers).extracting(ConversationMessage::getError).containsExactly(AnswerError.MODEL_ERROR, AnswerError.MODEL_ERROR);
		assertThat(answers).extracting(ConversationMessage::getText).containsExactly(null, "Je commence par");
	}

	@Test
	void eachStepTellsTheModelTheRemarksItCreated() {
		replies.add(() -> calls(
				call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 0}"),
				call("2", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 500}")));
		replies.add(() -> calls(call("3", "note_proposal", """
				{"summary": "Deux caissons", "openQuestions": [], "assumptions": [],
				 "explanations": [{"remark": "GAP:cabinet-1,cabinet-2", "why": "Le client veut une niche à poubelle."}]}""")));
		replies.add(() -> text("Deux caissons, avec une niche entre eux."));

		domain.send(projectId, SendMessageRequest.builder().text("Deux caissons").build(), List.of(), principal);

		var results = requests.get(1).getMessages().stream().filter(message -> message.speaker() == Speaker.TOOL)
				.map(message -> message.toolResult().content()).toList();
		assertThat(results.get(0)).doesNotContain("newRemarks");
		assertThat(results.get(1)).contains("newRemarks", "GAP:cabinet-1,cabinet-2", "Gap of 1.625");
		var noted = requests.get(2).getMessages().get(requests.get(2).getMessages().size() - 1).toolResult().content();
		assertThat(noted).contains("\"unexplained\":0", "niche");
		var proposal = domain.view(projectId, 0, principal).getProposal();
		assertThat(proposal.getRemarks()).extracting(Remark::getCode).containsExactly("GAP");
		assertThat(proposal.getRemarkExplanations()).containsEntry("GAP:cabinet-1,cabinet-2", "Le client veut une niche à poubelle.");
		assertThat(requests).hasSize(3);
	}

	@Test
	void unexplainedRemarksGetOneReminder() {
		replies.add(() -> calls(
				call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 0}"),
				call("2", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 500}")));
		replies.add(() -> text("Voilà."));
		replies.add(() -> text("Toujours voilà."));

		domain.send(projectId, SendMessageRequest.builder().text("Deux caissons").build(), List.of(), principal);

		assertThat(requests).hasSize(3);
		var reminder = requests.get(2).getMessages().get(requests.get(2).getMessages().size() - 1);
		assertThat(reminder.speaker()).isEqualTo(Speaker.MEMBER);
		assertThat(reminder.text()).contains("without an explanation", "GAP:cabinet-1,cabinet-2");
		var view = domain.view(projectId, 0, principal);
		assertThat(view.getMessages().get(1).getText()).isEqualTo("Toujours voilà.");
		assertThat(view.getProposal().getRemarkExplanations()).isEmpty();
		assertThat(view.getProposal().getRemarks()).hasSize(1);
	}

	@Test
	void theInstructionsCarryTheGuideTheShopStyleAndTheRemarksAlreadyThere() {
		when(assistantSettings.findSettings(member.getShopId())).thenReturn(Optional.of(AssistantSettings.builder()
				.styleText("Pas de coins aveugles.").build()));
		project = project.toBuilder().design(project.getDesign().toBuilder()
				.cabinets(List.of(cabinet("c0", 0), cabinet("c1", 500))).build()).build();
		replies.add(() -> text("Bonjour."));

		domain.send(projectId, SendMessageRequest.builder().text("Bonjour").build(), List.of(), principal);

		assertThat(requests.get(0).getInstructions()).contains("Design guide", "Landing space", "beside the range at least 12\"",
				"Pas de coins aveugles.", "comes before the design guide", "GAP:c0,c1");
	}

	@Test
	void answersOneRequestAtATime() {
		repository.create(Conversation.builder().id(UUID.randomUUID()).shopId(member.getShopId()).projectId(projectId)
				.creationTimestamp(LocalDateTime.now()).build());
		repository.startRunning(repository.findByProject(projectId, member.getShopId()).orElseThrow().getId());

		assertThatThrownBy(() -> domain.send(projectId, SendMessageRequest.builder().text("Encore").build(), List.of(), principal))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("still answering");
	}

	@Test
	void refusesMessagesWhileTheAssistantIsOff() {
		when(assistantDomain.isAvailable(member.getShopId())).thenReturn(false);

		assertThatThrownBy(() -> domain.send(projectId, SendMessageRequest.builder().text("Bonjour").build(), List.of(), principal))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("off");
	}

	@Test
	void acceptingReplaysTheStepsOntoTheCurrentDesign() {
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> text("Un caisson."));
		domain.send(projectId, SendMessageRequest.builder().text("Un caisson").build(), List.of(), principal);
		var proposal = domain.view(projectId, 0, principal).getProposal();
		// A colleague placed a cabinet at the wall's start since: the new one goes past it.
		project = project.toBuilder().design(project.getDesign().toBuilder().cabinets(List.of(cabinet("c0", 0))).build()).build();

		var result = domain.accept(projectId, proposal.getId(), principal);

		assertThat(result.getAppliedSteps()).isEqualTo(1);
		assertThat(result.getRefusedStep()).isNull();
		assertThat(project.getDesign().getCabinets()).extracting(Cabinet::getId)
				.containsExactly("c0", proposal.getSteps().get(0).getCreatedId());
		assertThat(project.getDesign().getCabinets().get(1).getOffsetMm()).isEqualTo(457.2);
		assertThat(result.getProposal().getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
		assertThat(result.getProposal().getDesignBeforeAccept().getCabinets()).extracting(Cabinet::getId).containsExactly("c0");
	}

	@Test
	void acceptingStopsAtTheFirstStepThatNowBreaksARule() {
		replies.add(() -> calls(
				call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 1000}"),
				call("2", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\", \"offsetMm\": 0}")));
		replies.add(() -> text("Deux caissons."));
		domain.send(projectId, SendMessageRequest.builder().text("Deux caissons").build(), List.of(), principal);
		var proposal = domain.view(projectId, 0, principal).getProposal();
		project = project.toBuilder().design(project.getDesign().toBuilder().cabinets(List.of(cabinet("c0", 0))).build()).build();

		var result = domain.accept(projectId, proposal.getId(), principal);

		assertThat(result.getAppliedSteps()).isEqualTo(1);
		assertThat(result.getTotalSteps()).isEqualTo(2);
		assertThat(result.getRefusedStep()).isEqualTo(1);
		assertThat(result.getViolations()).extracting("code").contains("OVERLAP");
		assertThat(project.getDesign().getCabinets()).hasSize(2);
	}

	@Test
	void undoingRestoresTheDesignUnlessTheProjectWasSavedSince() {
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> text("Un caisson."));
		domain.send(projectId, SendMessageRequest.builder().text("Un caisson").build(), List.of(), principal);
		var proposal = domain.view(projectId, 0, principal).getProposal();
		var before = project.getDesign();
		domain.accept(projectId, proposal.getId(), principal);

		var undone = domain.undoAccept(projectId, proposal.getId(), principal);

		assertThat(project.getDesign()).isEqualTo(before);
		assertThat(undone.getProposal().getStatus()).isEqualTo(ProposalStatus.OPEN);

		domain.accept(projectId, proposal.getId(), principal);
		project = project.toBuilder().updateTimestamp(project.getUpdateTimestamp().plusHours(1)).build();
		assertThatThrownBy(() -> domain.undoAccept(projectId, proposal.getId(), principal))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("saved since");
	}

	@Test
	void anotherMembersLockStopsAccepting() {
		replies.add(() -> calls(call("1", "add_cabinet", "{\"templateCode\": \"BASE_DOOR\", \"wallId\": \"w1\"}")));
		replies.add(() -> text("Un caisson."));
		domain.send(projectId, SendMessageRequest.builder().text("Un caisson").build(), List.of(), principal);
		var proposal = domain.view(projectId, 0, principal).getProposal();
		when(projectDomain.lock(projectId, principal)).thenReturn(ProjectLock.builder().mine(false).holderName("Luc").build());

		assertThatThrownBy(() -> domain.accept(projectId, proposal.getId(), principal))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Luc");
		verify(projectDomain, never()).update(any(), any(), any());
	}

	@Test
	void closesRequestsARestartCutShort() {
		var conversation = repository.create(Conversation.builder().id(UUID.randomUUID()).shopId(member.getShopId())
				.projectId(projectId).creationTimestamp(LocalDateTime.now()).build());
		repository.startRunning(conversation.getId());

		assertThat(domain.interruptRunning()).isEqualTo(1);

		var view = domain.view(projectId, 0, principal);
		assertThat(view.isRunning()).isFalse();
		assertThat(view.getMessages()).extracting(ConversationMessage::getError).containsExactly(AnswerError.INTERRUPTED);
	}

	private static ModelReply calls(ToolCall... calls) {
		return ModelReply.builder().toolCalls(List.of(calls)).model("fake").costUsd(BigDecimal.ZERO).build();
	}

	private static ModelReply text(String text) {
		return ModelReply.builder().text(text).model("fake").costUsd(BigDecimal.ZERO).build();
	}

	private static ToolCall call(String id, String name, String arguments) {
		return new ToolCall(id, name, arguments);
	}

	private static Cabinet cabinet(String id, double offsetMm) {
		return Cabinet.builder().id(id).templateCode("BASE_DOOR").templateVersion(2).wallId("w1").offsetMm(offsetMm)
				.widthMm(457.2).heightMm(876.3).depthMm(609.6).shelfCount(1).build();
	}

	private static List<CabinetTemplate> templates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return JSON.readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}



	/**
	 * Ids numbered in order, so tests can name what the assistant created.
	 */
	private static final class SequentialIds extends DesignIds {
		private final Map<String, Integer> counts = new HashMap<>();

		@Override
		public String next(String prefix) {
			return prefix + "-" + counts.merge(prefix, 1, Integer::sum);
		}
	}
}
