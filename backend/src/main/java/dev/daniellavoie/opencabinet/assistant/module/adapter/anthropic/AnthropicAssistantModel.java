package dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelReply;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;
import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantModel;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Claude through Spring AI. The chat model exists only when an API key is configured.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnthropicAssistantModel implements AssistantModel {
	static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

	AnthropicConfiguration configuration;
	Optional<AnthropicChatModel> chatModel;

	public AnthropicAssistantModel(AnthropicConfiguration configuration) {
		this.configuration = configuration;
		this.chatModel = Optional.ofNullable(configuration.getApiKey())
				.filter(key -> !key.isBlank())
				.map(key -> AnthropicChatModel.builder()
						.options(AnthropicChatOptions.builder().apiKey(key).baseUrl(configuration.getBaseUrl()).build())
						.build());
		for (var tier : ModelTier.values()) {
			prices(modelFor(tier));
		}
		log.info("Design assistant model {} with: { builderModel = {}, sorterModel = {} }",
				chatModel.isPresent() ? "configured" : "not configured", configuration.getBuilderModel(),
				configuration.getSorterModel());
	}

	@Override
	public boolean isConfigured() {
		return chatModel.isPresent();
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
		var messages = new ArrayList<Message>();
		messages.add(new SystemMessage(request.getInstructions()));
		request.getMessages().forEach(message -> messages.add(message.speaker() == Speaker.MEMBER
				? new UserMessage(message.text())
				: new AssistantMessage(message.text())));

		var response = chatModel.orElseThrow(() -> new IllegalStateException("the server has no model provider key"))
				.call(new Prompt(messages, AnthropicChatOptions.builder()
						.model(model)
						.maxTokens(request.getMaxTokens())
						.build()));

		var usage = response.getMetadata().getUsage();
		long input = Optional.ofNullable(usage.getPromptTokens()).orElse(0);
		long output = Optional.ofNullable(usage.getCompletionTokens()).orElse(0);
		return ModelReply.builder()
				.text(response.getResult().getOutput().getText())
				.model(model)
				.inputTokens(input)
				.outputTokens(output)
				.costUsd(cost(prices(model), input, output))
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
