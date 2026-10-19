package dev.daniellavoie.opencabinet.assistant.domain;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import dev.daniellavoie.opencabinet.assistant.domain.model.AcceptResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.AnswerError;
import dev.daniellavoie.opencabinet.assistant.domain.model.Attachment;
import dev.daniellavoie.opencabinet.assistant.domain.model.AttachmentContent;
import dev.daniellavoie.opencabinet.assistant.domain.model.Author;
import dev.daniellavoie.opencabinet.assistant.domain.model.BudgetSpentException;
import dev.daniellavoie.opencabinet.assistant.domain.model.Conversation;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ConversationView;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignProposal;
import dev.daniellavoie.opencabinet.assistant.domain.model.DesignStep;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ModelMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolSpec;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProgressEvent;
import dev.daniellavoie.opencabinet.assistant.domain.model.ProposalStatus;
import dev.daniellavoie.opencabinet.assistant.domain.model.SendMessageRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.UploadedFile;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ModelAttachment;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.AssistantDomain;
import dev.daniellavoie.opencabinet.assistant.domain.port.api.ConversationDomain;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantJobs;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AttachmentStore;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.PdfInspector;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.ConversationRepository;
import dev.daniellavoie.opencabinet.assistant.domain.tool.DesignToolContext;
import dev.daniellavoie.opencabinet.assistant.domain.tool.DesignTools;
import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.member.domain.port.backend.MemberRepository;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.UpdateProjectRequest;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * The Design Assistant's Conversations (ADR 0004). Each request runs in the background: the model
 * asks for tool calls, each becomes a step of the Project's open Design Proposal when it passes the
 * Construction Rules, and the domain answers each call before asking the model again. Every round
 * goes through the assistant's gate, so the Shop's opt-in and budget hold mid-request.
 */
@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultConversationDomain implements ConversationDomain {
	static final int MAX_ROUNDS = 25;
	static final int MAX_TOKENS = 8192;
	static final int MAX_MESSAGE_LENGTH = 20_000;
	static final int HISTORY = 30;

	static final String VIEW_DESIGN = "view_design";
	static final String NOTE_PROPOSAL = "note_proposal";

	static final List<ToolSpec> TOOLS = Stream.concat(
			DesignTools.ALL.stream().map(tool -> new ToolSpec(tool.name(), tool.description(), tool.inputSchema())),
			Stream.of(
					new ToolSpec(VIEW_DESIGN, "Returns the proposal's design as it is now.",
							"{\"type\": \"object\", \"properties\": {}, \"additionalProperties\": false}"),
					new ToolSpec(NOTE_PROPOSAL, """
							Records the proposal's short summary, the questions the member should answer, and the \
							assumptions made. Replaces what was noted before.""", """
							{"type": "object", "properties": {
							"summary": {"type": "string"},
							"openQuestions": {"type": "array", "items": {"type": "string"}},
							"assumptions": {"type": "array", "items": {"type": "string"}}
							}, "required": ["summary", "openQuestions", "assumptions"], "additionalProperties": false}""")))
			.toList();

	MemberDomain memberDomain;
	MemberRepository memberRepository;
	ProjectDomain projectDomain;
	ClientDomain clientDomain;
	CatalogDomain catalogDomain;
	ShopRepository shopRepository;
	AssistantDomain assistantDomain;
	ConversationRepository repository;
	ConversationProgress progress;
	AttachmentStore attachments;
	PdfInspector pdfs;
	DesignIds designIds;
	AssistantJobs jobs;
	JsonMapper jsonMapper;

	@Override
	public ConversationView view(UUID projectId, int afterProgress, Principal principal) {
		var member = memberDomain.current(principal);
		projectDomain.findById(projectId, member.getShopId());
		return view(projectId, member.getShopId(), afterProgress);
	}

	@Override
	public ConversationView send(UUID projectId, SendMessageRequest request, List<UploadedFile> files, Principal principal) {
		var member = memberDomain.current(principal);
		var shopId = member.getShopId();
		projectDomain.findById(projectId, shopId);
		var text = Optional.ofNullable(request.getText()).map(String::strip).filter(value -> !value.isEmpty())
				.orElseThrow(() -> new IllegalArgumentException("a message is required"));
		if (text.length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("a message is at most " + MAX_MESSAGE_LENGTH + " characters");
		}
		var checked = Attachments.check(Optional.ofNullable(files).orElse(List.of()), pdfs);
		if (!assistantDomain.isAvailable(shopId)) {
			throw new IllegalStateException("the design assistant is off for this shop");
		}

		var conversation = repository.findByProject(projectId, shopId)
				.orElseGet(() -> repository.create(Conversation.builder()
						.id(UUID.randomUUID())
						.shopId(shopId)
						.projectId(projectId)
						.creationTimestamp(LocalDateTime.now())
						.build()));
		if (!repository.startRunning(conversation.getId())) {
			throw new IllegalStateException("the assistant is still answering the last message");
		}
		var message = repository.addMessage(ConversationMessage.builder()
				.id(UUID.randomUUID())
				.conversationId(conversation.getId())
				.author(Author.MEMBER)
				.memberId(member.getId())
				.text(text)
				.creationTimestamp(LocalDateTime.now())
				.build());
		for (var file : checked) {
			attachments.save(Attachment.builder()
					.id(UUID.randomUUID())
					.shopId(shopId)
					.conversationId(conversation.getId())
					.messageId(message.getId())
					.fileName(file.fileName())
					.mediaType(file.mediaType())
					.creationTimestamp(LocalDateTime.now())
					.build(), file.bytes());
		}
		progress.start(conversation.getId());

		var language = "en".equals(request.getLanguage()) ? "en" : "fr";
		jobs.run(() -> answer(conversation, member.getId(), language));
		return view(projectId, shopId, 0);
	}

	/**
	 * Answers the Conversation's last message: asks the model, takes the steps it calls for, and
	 * saves the Proposal and the answer, even when stopped early.
	 */
	void answer(Conversation conversation, UUID memberId, String language) {
		var shopId = conversation.getShopId();
		var session = (Session) null;
		String text = null;
		AnswerError error = null;
		try {
			session = start(conversation, language);
			var outcome = converse(session, memberId);
			text = outcome.text();
			error = outcome.error();
		} catch (BudgetSpentException exception) {
			error = AnswerError.BUDGET_SPENT;
		} catch (IllegalStateException exception) {
			error = assistantDomain.isAvailable(shopId) ? AnswerError.MODEL_ERROR : AnswerError.OFF;
			log.warn("Design assistant stopped for conversation {}: {}", conversation.getId(), exception.getMessage());
		} catch (RuntimeException exception) {
			error = AnswerError.MODEL_ERROR;
			log.error("Design assistant failed for conversation {}", conversation.getId(), exception);
		}

		try {
			var proposalId = session == null ? null : saveProposal(session);
			repository.addMessage(ConversationMessage.builder()
					.id(UUID.randomUUID())
					.conversationId(conversation.getId())
					.author(Author.ASSISTANT)
					.text(text)
					.error(error)
					.proposalId(proposalId)
					.creationTimestamp(LocalDateTime.now())
					.build());
		} finally {
			repository.stopRunning(conversation.getId());
		}
	}

	/**
	 * A request's working state: the design and steps it builds on, what it noted, and the messages
	 * exchanged with the model.
	 */
	private static final class Session {
		Conversation conversation;
		Project project;
		DesignProposal open;
		Design design;
		List<DesignStep> steps;
		String summary;
		List<String> openQuestions;
		List<String> assumptions;
		boolean changed;
		DesignToolContext context;
		String instructions;
		List<ModelMessage> messages;
	}

	private record Outcome(String text, AnswerError error) {
	}

	private Session start(Conversation conversation, String language) {
		var shopId = conversation.getShopId();
		var session = new Session();
		session.conversation = conversation;
		session.project = projectDomain.findById(conversation.getProjectId(), shopId);
		session.open = repository.findOpenProposal(conversation.getProjectId(), shopId).orElse(null);
		session.design = session.open != null ? session.open.getDesign() : session.project.getDesign();
		session.steps = new ArrayList<>(session.open != null ? session.open.getSteps() : List.of());
		session.summary = session.open != null ? session.open.getSummary() : null;
		session.openQuestions = session.open != null ? session.open.getOpenQuestions() : List.of();
		session.assumptions = session.open != null ? session.open.getAssumptions() : List.of();
		session.context = context(session.project, shopId, designIds::next);

		var shop = shopRepository.findById(shopId);
		var materials = catalogDomain.findByShop(shopId);
		var names = materials.stream().collect(Collectors.toMap(Material::getId, Material::getName));
		var standard = shop.getSettings().getConstructionStandard();
		var standardMaterials = new HashMap<String, String>();
		Optional.ofNullable(standard.getCarcassMaterialId()).map(names::get).ifPresent(name -> standardMaterials.put("carcass", name));
		Optional.ofNullable(standard.getBackMaterialId()).map(names::get).ifPresent(name -> standardMaterials.put("back", name));
		Optional.ofNullable(standard.getFrontMaterialId()).map(names::get).ifPresent(name -> standardMaterials.put("fronts", name));
		Optional.ofNullable(standard.getOrderedFrontId()).map(names::get).ifPresent(name -> standardMaterials.put("orderedFronts", name));

		session.instructions = AssistantInstructions.build(new AssistantInstructions.Context(language, shop.getUnits(),
				session.project.getName(), clientDomain.findById(session.project.getClientId(), shopId).getName(), standard,
				standardMaterials, List.copyOf(session.context.frontStyles().keySet()),
				List.copyOf(session.context.addableTemplates().values()), session.design, session.open != null), jsonMapper);

		var history = repository.findMessages(conversation.getId()).stream()
				.filter(message -> message.getText() != null)
				.toList();
		var recent = history.subList(Math.max(0, history.size() - HISTORY), history.size());
		var files = attachments.contents(recent.stream()
				.filter(message -> message.getAuthor() == Author.MEMBER)
				.map(ConversationMessage::getId)
				.toList());
		session.messages = new ArrayList<>(recent.stream()
				.map(message -> message.getAuthor() == Author.MEMBER
						? ModelMessage.member(message.getText(), files.getOrDefault(message.getId(), List.of()).stream()
								.map(file -> new ModelAttachment(file.mediaType(), file.bytes()))
								.toList())
						: new ModelMessage(Speaker.ASSISTANT, message.getText()))
				.toList());
		return session;
	}

	private Outcome converse(Session session, UUID memberId) {
		var conversationId = session.conversation.getId();
		for (var round = 0; round < MAX_ROUNDS; round++) {
			progress.add(conversationId, ProgressEvent.builder().kind(ProgressEvent.Kind.THINKING));
			var reply = assistantDomain.complete(ModelRequest.builder()
					.tier(ModelTier.BUILDER)
					.purpose("PROPOSAL")
					.instructions(session.instructions)
					.messages(List.copyOf(session.messages))
					.tools(TOOLS)
					.maxTokens(MAX_TOKENS)
					.build(), session.conversation.getShopId(), memberId);
			session.messages.add(ModelMessage.assistant(reply.getText(), reply.getToolCalls(), reply.getProviderState()));
			if (reply.getToolCalls().isEmpty()) {
				if (reply.isTruncated() || reply.getText() == null || reply.getText().isBlank()) {
					// Cut short, or nothing at all: say so rather than keep an empty answer.
					log.warn("Design assistant gave no usable answer for conversation {} with: { truncated = {}, round = {} }",
							session.conversation.getId(), reply.isTruncated(), round + 1);
					return new Outcome(blankToNull(reply.getText()), AnswerError.MODEL_ERROR);
				}
				return new Outcome(reply.getText(), null);
			}
			for (var call : reply.getToolCalls()) {
				session.messages.add(ModelMessage.tool(new ToolResult(call.id(), call.name(), run(session, call))));
			}
		}
		return new Outcome(null, AnswerError.TOO_MANY_STEPS);
	}

	/**
	 * Runs one tool call and answers the model, as JSON.
	 */
	private String run(Session session, ToolCall call) {
		var conversationId = session.conversation.getId();
		JsonNode arguments;
		try {
			arguments = Optional.ofNullable(call.arguments()).filter(value -> !value.isBlank())
					.map(jsonMapper::readTree).orElseGet(jsonMapper::createObjectNode);
		} catch (RuntimeException exception) {
			return answer(false, Map.of("problem", "the arguments are not valid JSON"));
		}

		if (VIEW_DESIGN.equals(call.name())) {
			return jsonMapper.writeValueAsString(session.design);
		}
		if (NOTE_PROPOSAL.equals(call.name())) {
			session.summary = Optional.ofNullable(arguments.get("summary")).map(JsonNode::asString).orElse(null);
			session.openQuestions = strings(arguments.get("openQuestions"));
			session.assumptions = strings(arguments.get("assumptions"));
			session.changed = true;
			progress.add(conversationId, ProgressEvent.builder().kind(ProgressEvent.Kind.NOTED).tool(call.name()));
			return answer(true, Map.of());
		}

		var result = ProposalSteps.take(session.design, call.name(), arguments, session.context);
		if (result instanceof ProposalSteps.Refused refused) {
			progress.add(conversationId, ProgressEvent.builder().kind(ProgressEvent.Kind.REFUSED).tool(call.name())
					.arguments(arguments).violations(refused.violations()).problem(refused.problem()));
			var details = new HashMap<String, Object>();
			if (refused.problem() != null) {
				details.put("problem", refused.problem());
			}
			if (!refused.violations().isEmpty()) {
				details.put("brokenRules", refused.violations());
				details.put("note", "the step was not taken; the design is unchanged");
			}
			return answer(false, details);
		}

		var taken = (ProposalSteps.Taken) result;
		session.design = taken.design();
		session.steps.add(taken.step());
		session.changed = true;
		progress.add(conversationId, ProgressEvent.builder().kind(ProgressEvent.Kind.STEP).tool(call.name())
				.arguments(arguments).createdId(taken.step().getCreatedId()));
		var details = new HashMap<String, Object>();
		if (taken.step().getCreatedId() != null) {
			details.put("createdId", taken.step().getCreatedId());
		}
		element(session.design, Optional.ofNullable(taken.step().getCreatedId())
				.or(() -> Stream.of("cabinetId", "itemId", "wallId")
						.map(arguments::get).filter(Objects::nonNull).map(JsonNode::asString).findFirst()))
				.ifPresent(element -> details.put("now", element));
		return answer(true, details);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text;
	}

	private String answer(boolean ok, Map<String, Object> details) {
		ObjectNode node = jsonMapper.createObjectNode();
		node.put("ok", ok);
		details.forEach((key, value) -> node.set(key, jsonMapper.valueToTree(value)));
		return jsonMapper.writeValueAsString(node);
	}

	/**
	 * The Wall, Cabinet or item with this id, as it is now.
	 */
	private static Optional<Object> element(Design design, Optional<String> id) {
		return id.flatMap(value -> Stream.of(
				Optional.ofNullable(design.getCabinets()).orElse(List.of()).stream().filter(item -> item.getId().equals(value)),
				Optional.ofNullable(design.getWalls()).orElse(List.of()).stream().filter(item -> item.getId().equals(value)),
				Optional.ofNullable(design.getOpenings()).orElse(List.of()).stream().filter(item -> item.getId().equals(value)),
				Optional.ofNullable(design.getAppliances()).orElse(List.of()).stream().filter(item -> item.getId().equals(value)),
				Optional.ofNullable(design.getFillers()).orElse(List.of()).stream().filter(item -> item.getId().equals(value)))
				.flatMap(found -> found.map(Object.class::cast))
				.findFirst());
	}

	private static List<String> strings(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		var values = new ArrayList<String>();
		node.forEach(value -> values.add(value.asString()));
		return List.copyOf(values);
	}

	/**
	 * Saves what the request changed: the open Proposal, or a new one from the Project's design.
	 */
	private UUID saveProposal(Session session) {
		if (!session.changed) {
			return null;
		}
		var now = LocalDateTime.now();
		var proposal = Optional.ofNullable(session.open)
				.map(DesignProposal::toBuilder)
				.orElseGet(() -> DesignProposal.builder()
						.id(UUID.randomUUID())
						.shopId(session.conversation.getShopId())
						.projectId(session.conversation.getProjectId())
						.conversationId(session.conversation.getId())
						.status(ProposalStatus.OPEN)
						.baseDesign(session.project.getDesign())
						.creationTimestamp(now))
				.design(session.design)
				.steps(List.copyOf(session.steps))
				.summary(session.summary)
				.openQuestions(session.openQuestions)
				.assumptions(session.assumptions)
				.updateTimestamp(now)
				.build();
		return repository.saveProposal(proposal).getId();
	}

	@Override
	public AttachmentContent attachment(UUID projectId, UUID attachmentId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		projectDomain.findById(projectId, shopId);
		return attachments.content(attachmentId, shopId)
				.orElseThrow(() -> new java.util.NoSuchElementException("attachment " + attachmentId + " not found"));
	}

	@Override
	public AcceptResult accept(UUID projectId, UUID proposalId, Principal principal) {
		var member = memberDomain.current(principal);
		var shopId = member.getShopId();
		var proposal = proposal(projectId, proposalId, shopId);
		if (proposal.getStatus() != ProposalStatus.OPEN) {
			throw new IllegalStateException("only an open proposal can be accepted");
		}
		if (proposal.getSteps().isEmpty()) {
			throw new IllegalStateException("this proposal changes nothing");
		}
		requireIdle(projectId, shopId);
		requireLock(projectId, principal);

		var project = projectDomain.findById(projectId, shopId);
		var replay = ProposalSteps.replay(project.getDesign(), proposal.getSteps(),
				context(project, shopId, designIds::next));
		var refusedViolations = replay.refusal() == null ? null : replay.refusal().violations();
		if (replay.applied() == 0) {
			return AcceptResult.builder()
					.project(project)
					.proposal(proposal)
					.appliedSteps(0)
					.totalSteps(proposal.getSteps().size())
					.refusedStep(replay.refusedStep())
					.violations(refusedViolations)
					.build();
		}

		projectDomain.update(projectId, request(project, replay.design()), principal);
		var saved = projectDomain.findById(projectId, shopId);
		var accepted = repository.saveProposal(proposal.toBuilder()
				.status(ProposalStatus.ACCEPTED)
				.acceptedBy(member.getId())
				.acceptedTimestamp(LocalDateTime.now())
				.designBeforeAccept(project.getDesign())
				.acceptedProjectTimestamp(saved.getUpdateTimestamp())
				.appliedSteps(replay.applied())
				.refusedStep(replay.refusedStep())
				.refusedViolations(refusedViolations)
				.updateTimestamp(LocalDateTime.now())
				.build());
		log.info("Accepted design proposal {} on project {} with: { appliedSteps = {}, totalSteps = {} }", proposalId, projectId,
				replay.applied(), proposal.getSteps().size());
		return AcceptResult.builder()
				.project(saved)
				.proposal(accepted)
				.appliedSteps(replay.applied())
				.totalSteps(proposal.getSteps().size())
				.refusedStep(replay.refusedStep())
				.violations(refusedViolations)
				.build();
	}

	@Override
	public AcceptResult undoAccept(UUID projectId, UUID proposalId, Principal principal) {
		var member = memberDomain.current(principal);
		var shopId = member.getShopId();
		var proposal = proposal(projectId, proposalId, shopId);
		if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
			throw new IllegalStateException("only an accepted proposal can be undone");
		}
		var project = projectDomain.findById(projectId, shopId);
		if (!Objects.equals(project.getUpdateTimestamp(), proposal.getAcceptedProjectTimestamp())) {
			throw new IllegalStateException("the project was saved since the proposal was accepted");
		}
		requireIdle(projectId, shopId);
		requireLock(projectId, principal);

		projectDomain.update(projectId, request(project, proposal.getDesignBeforeAccept()), principal);
		var reopened = repository.saveProposal(proposal.toBuilder()
				.status(ProposalStatus.OPEN)
				.acceptedBy(null)
				.acceptedTimestamp(null)
				.designBeforeAccept(null)
				.acceptedProjectTimestamp(null)
				.appliedSteps(null)
				.refusedStep(null)
				.refusedViolations(null)
				.updateTimestamp(LocalDateTime.now())
				.build());
		return AcceptResult.builder()
				.project(projectDomain.findById(projectId, shopId))
				.proposal(reopened)
				.appliedSteps(0)
				.totalSteps(proposal.getSteps().size())
				.build();
	}

	@Override
	public DesignProposal discard(UUID projectId, UUID proposalId, Principal principal) {
		var shopId = memberDomain.current(principal).getShopId();
		var proposal = proposal(projectId, proposalId, shopId);
		if (proposal.getStatus() != ProposalStatus.OPEN) {
			throw new IllegalStateException("only an open proposal can be discarded");
		}
		requireIdle(projectId, shopId);
		return repository.saveProposal(proposal.toBuilder()
				.status(ProposalStatus.DISCARDED)
				.updateTimestamp(LocalDateTime.now())
				.build());
	}

	@Override
	public int interruptRunning() {
		var running = repository.findRunning();
		for (var conversation : running) {
			repository.addMessage(ConversationMessage.builder()
					.id(UUID.randomUUID())
					.conversationId(conversation.getId())
					.author(Author.ASSISTANT)
					.error(AnswerError.INTERRUPTED)
					.creationTimestamp(LocalDateTime.now())
					.build());
			repository.stopRunning(conversation.getId());
		}
		return running.size();
	}

	private ConversationView view(UUID projectId, UUID shopId, int afterProgress) {
		var conversation = repository.findByProject(projectId, shopId);
		var names = new HashMap<UUID, String>();
		var found = conversation.map(existing -> repository.findMessages(existing.getId())).orElse(List.of());
		var files = attachments.findByMessages(found.stream().map(ConversationMessage::getId).toList());
		var messages = found.stream()
				.map(message -> message.toBuilder()
						.memberName(message.getMemberId() == null ? null
								: names.computeIfAbsent(message.getMemberId(), id -> memberRepository.findById(id, shopId).getName()))
						.attachments(files.getOrDefault(message.getId(), List.of()))
						.build())
				.toList();
		return ConversationView.builder()
				.conversationId(conversation.map(Conversation::getId).orElse(null))
				.running(conversation.map(Conversation::isRunning).orElse(false))
				.messages(messages)
				.progress(conversation.map(existing -> progress.since(existing.getId(), afterProgress)).orElse(List.of()))
				.proposal(repository.findCurrentProposal(projectId, shopId).orElse(null))
				.build();
	}

	/**
	 * What tools use: the Project's templates, the newest non-archived version of each, and the
	 * Shop's ordered Front styles by name.
	 */
	private DesignToolContext context(Project project, UUID shopId, Function<String, String> newId) {
		var templates = projectDomain.templates(project);
		var newest = templates.stream()
				.collect(Collectors.groupingBy(CabinetTemplate::getCode,
						Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparingInt(CabinetTemplate::getVersion)),
								Optional::orElseThrow)))
				.values().stream()
				.filter(template -> !Boolean.TRUE.equals(template.getArchived()))
				.sorted(Comparator.comparing(CabinetTemplate::getCode))
				.collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity(), (first, second) -> first,
						java.util.LinkedHashMap::new));
		var frontStyles = catalogDomain.findByShop(shopId).stream()
				.filter(material -> material.getKind() == MaterialKind.FRONT)
				.collect(Collectors.toMap(Material::getName, Material::getId, (first, second) -> first,
						java.util.LinkedHashMap::new));
		return new DesignToolContext(templates, newest, frontStyles, newId);
	}

	private DesignProposal proposal(UUID projectId, UUID proposalId, UUID shopId) {
		var proposal = repository.findProposal(proposalId, shopId);
		if (!proposal.getProjectId().equals(projectId)) {
			throw new java.util.NoSuchElementException("design proposal " + proposalId + " not found");
		}
		return proposal;
	}

	private void requireIdle(UUID projectId, UUID shopId) {
		if (repository.findByProject(projectId, shopId).map(Conversation::isRunning).orElse(false)) {
			throw new IllegalStateException("the assistant is still working on this proposal");
		}
	}

	private void requireLock(UUID projectId, Principal principal) {
		var lock = projectDomain.lock(projectId, principal);
		if (!lock.isMine()) {
			throw new IllegalStateException("this project is being edited by " + lock.getHolderName());
		}
	}

	private static UpdateProjectRequest request(Project project, Design design) {
		return UpdateProjectRequest.builder()
				.clientId(project.getClientId())
				.name(project.getName())
				.design(design)
				.labourMethod(project.getLabourMethod())
				.nestingLayout(project.getNestingLayout())
				.labourHours(project.getLabourHours())
				.manualLines(project.getManualLines())
				.build();
	}
}
