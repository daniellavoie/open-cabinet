package dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * The Design Assistant's model provider. Without an API key the assistant cannot be turned on.
 */
@Data
@Configuration
@ConfigurationProperties("open-cabinet.assistant.anthropic")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnthropicConfiguration {
	String apiKey;

	/**
	 * The provider's address when not the default, such as a proxy.
	 */
	String baseUrl;

	/**
	 * Builds Design Proposals.
	 */
	String builderModel;

	/**
	 * Sorts the Inbox.
	 */
	String sorterModel;

	/**
	 * US dollars per million tokens, by model: a model without prices is refused, so no call goes
	 * uncounted.
	 */
	Map<String, Prices> prices = Map.of();

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class Prices {
		BigDecimal inputPerMillion;
		BigDecimal outputPerMillion;
	}
}
