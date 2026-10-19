package dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.ThinkingConfigBetweenTools;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ModelMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolSpec;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Claude through Anthropic's Java SDK, which the client exists for only when an API key is
 * configured. The model writes short notes between tool calls as thinking blocks; each answer is
 * handed back as it came, so they go back with the tool results as the model needs.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnthropicAssistantModel implements AssistantModel {
	static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
	static final Duration TIMEOUT = Duration.ofMinutes(5);
	static final JsonMapper JSON = JsonMapper.builder().build();

	AnthropicConfiguration configuration;
	Optional<AnthropicClient> client;

	public AnthropicAssistantModel(AnthropicConfiguration configuration) {
		this.configuration = configuration;
		this.client = Optional.ofNullable(configuration.getApiKey())
				.filter(key -> !key.isBlank())
				.map(key -> {
					var builder = AnthropicOkHttpClient.builder().apiKey(key).timeout(TIMEOUT).maxRetries(2);
					Optional.ofNullable(configuration.getBaseUrl()).filter(url -> !url.isBlank()).ifPresent(builder::baseUrl);
					return builder.build();
				});
		for (var tier : ModelTier.values()) {
			prices(modelFor(tier));
		}
		log.info("Design assistant model {} with: { builderModel = {}, sorterModel = {} }",
				client.isPresent() ? "configured" : "not configured", configuration.getBuilderModel(),
				configuration.getSorterModel());
	}

	@Override
	public boolean isConfigured() {
		return client.isPresent();
	}

	@Override
	public String modelFor(ModelTier tier) {
		return switch (tier) {
		case BUILDER -> configuration.getBuilderModel();
		case SORTER -> configuration.getSorterModel();
		};
	}

	@Override
	public ModelReply complete(ModelRequest request) {
		var model = modelFor(request.getTier());
		var params = MessageCreateParams.builder()
				.model(model)
				.maxTokens(request.getMaxTokens())
				.system(request.getInstructions())
				.messages(messages(request))
				// No thinking before answering; the notes written between tool calls come back as thinking.
				.thinking(ThinkingConfigBetweenTools.builder().build());
		request.getTools().forEach(tool -> params.addTool(tool(tool)));

		var message = client.orElseThrow(() -> new IllegalStateException("the server has no model provider key"))
				.messages()
				.create(params.build());

		var text = message.content().stream()
				.filter(block -> block.isText())
				.map(block -> block.asText().text())
				.filter(value -> !value.isBlank())
				.collect(Collectors.joining("\n\n"));
		var toolCalls = message.content().stream()
				.filter(block -> block.isToolUse())
				.map(block -> block.asToolUse())
				.map(call -> new ToolCall(call.id(), call.name(), JSON.writeValueAsString(call._input().convert(Object.class))))
				.toList();
		var input = message.usage().inputTokens();
		var output = message.usage().outputTokens();
		var stopReason = message.stopReason().map(StopReason::asString).orElse("");
		log.info("Design assistant reply from {} with: { stopReason = {}, blocks = {}, textLength = {}, toolCalls = {}, inputTokens = {}, outputTokens = {} }",
				model, stopReason, message.content().size(), text.length(), toolCalls.size(), input, output);
		return ModelReply.builder()
				.text(text.isEmpty() ? null : text)
				.toolCalls(toolCalls)
				.truncated(message.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false))
				.providerState(message)
				.model(model)
				.inputTokens(input)
				.outputTokens(output)
				.costUsd(cost(prices(model), input, output))
				.build();
	}

	/**
	 * The conversation; consecutive tool results travel together, answering the tool calls of the
	 * answer before them, and an answer goes back exactly as the model gave it.
	 */
	private static List<MessageParam> messages(ModelRequest request) {
		var messages = new ArrayList<MessageParam>();
		var results = new ArrayList<ContentBlockParam>();
		for (var message : request.getMessages()) {
			if (message.speaker() != Speaker.TOOL && !results.isEmpty()) {
				messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(List.copyOf(results)).build());
				results.clear();
			}
			switch (message.speaker()) {
			case MEMBER -> messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(member(message)).build());
			case ASSISTANT -> messages.add(message.providerState() instanceof Message answer ? answer.toParam() : assistant(message));
			case TOOL -> results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
					.toolUseId(message.toolResult().callId())
					.content(message.toolResult().content())
					.build()));
			}
		}
		if (!results.isEmpty()) {
			messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(List.copyOf(results)).build());
		}
		return messages;
	}

	/**
	 * The Member's text, after their photos and PDFs.
	 */
	private static List<ContentBlockParam> member(ModelMessage message) {
		var blocks = new ArrayList<ContentBlockParam>();
		for (var attachment : message.attachments()) {
			var data = Base64.getEncoder().encodeToString(attachment.bytes());
			blocks.add(attachment.mediaType().equals("application/pdf")
					? ContentBlockParam.ofDocument(Base64PdfSource.builder().data(data).build())
					: ContentBlockParam.ofImage(ImageBlockParam.builder()
							.source(Base64ImageSource.builder()
									.data(data)
									.mediaType(Base64ImageSource.MediaType.of(attachment.mediaType()))
									.build())
							.build()));
		}
		blocks.add(ContentBlockParam.ofText(message.text()));
		return blocks;
	}

	/**
	 * An earlier answer, from its text and tool calls, when it did not come from this provider.
	 */
	private static MessageParam assistant(ModelMessage message) {
		var blocks = new ArrayList<ContentBlockParam>();
		Optional.ofNullable(message.text()).filter(text -> !text.isBlank()).ifPresent(text -> blocks.add(ContentBlockParam.ofText(text)));
		message.toolCalls().forEach(call -> blocks.add(ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
				.id(call.id())
				.name(call.name())
				.input(JsonValue.from(JSON.readValue(call.arguments(), new TypeReference<Map<String, Object>>() {
				})))
				.build())));
		return MessageParam.builder().role(MessageParam.Role.ASSISTANT).contentOfBlockParams(blocks).build();
	}

	/**
	 * A tool the model may ask for; the domain runs each call itself.
	 */
	private static Tool tool(ToolSpec spec) {
		Map<String, Object> schema = JSON.readValue(spec.inputSchema(), new TypeReference<Map<String, Object>>() {
		});
		var properties = Tool.InputSchema.Properties.builder();
		@SuppressWarnings("unchecked")
		var declared = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
		declared.forEach((name, property) -> properties.putAdditionalProperty(name, JsonValue.from(property)));
		var inputSchema = Tool.InputSchema.builder().properties(properties.build());
		@SuppressWarnings("unchecked")
		var required = (List<String>) schema.getOrDefault("required", List.of());
		inputSchema.required(required);
		Optional.ofNullable(schema.get("additionalProperties"))
				.ifPresent(value -> inputSchema.putAdditionalProperty("additionalProperties", JsonValue.from(value)));
		return Tool.builder()
				.name(spec.name())
				.description(spec.description())
				.inputSchema(inputSchema.build())
				.build();
	}

	/**
	 * What the tokens cost, rounded up to the millionth of a dollar so the budget is never undercounted.
	 */
	static BigDecimal cost(AnthropicConfiguration.Prices prices, long inputTokens, long outputTokens) {
		return prices.getInputPerMillion().multiply(BigDecimal.valueOf(inputTokens))
				.add(prices.getOutputPerMillion().multiply(BigDecimal.valueOf(outputTokens)))
				.divide(MILLION, 6, RoundingMode.UP);
	}

	private AnthropicConfiguration.Prices prices(String model) {
		return Optional.ofNullable(configuration.getPrices().get(model))
				.filter(prices -> prices.getInputPerMillion() != null && prices.getOutputPerMillion() != null)
				.orElseThrow(() -> new IllegalStateException("no prices configured for model " + model));
	}
}
