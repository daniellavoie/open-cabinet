package dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelTier;

class AnthropicAssistantModelTest {
	@Test
	void costsTokensAtTheModelsPricesRoundedUp() {
		var prices = prices("3", "15");

		assertThat(AnthropicAssistantModel.cost(prices, 2_000, 500)).isEqualByComparingTo("0.0135");
		assertThat(AnthropicAssistantModel.cost(prices, 1, 0)).isEqualByComparingTo("0.000003");
		assertThat(AnthropicAssistantModel.cost(prices("1", "5"), 0, 1)).isEqualByComparingTo("0.000005");
		assertThat(AnthropicAssistantModel.cost(prices("0.1", "0.1"), 1, 0)).isEqualByComparingTo("0.000001");
	}

	@Test
	void withoutAKeyItIsNotConfigured() {
		var model = new AnthropicAssistantModel(configuration(""));

		assertThat(model.isConfigured()).isFalse();
		assertThat(model.modelFor(ModelTier.BUILDER)).isEqualTo("builder");
		assertThat(model.modelFor(ModelTier.SORTER)).isEqualTo("sorter");
	}

	@Test
	void aModelWithoutPricesIsRefusedAtStartup() {
		var configuration = configuration("");
		configuration.setPrices(Map.of("builder", prices("3", "15")));

		assertThatThrownBy(() -> new AnthropicAssistantModel(configuration))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sorter");
	}

	private static AnthropicConfiguration configuration(String apiKey) {
		var configuration = new AnthropicConfiguration();
		configuration.setApiKey(apiKey);
		configuration.setBuilderModel("builder");
		configuration.setSorterModel("sorter");
		configuration.setPrices(Map.of("builder", prices("3", "15"), "sorter", prices("1", "5")));
		return configuration;
	}

	private static AnthropicConfiguration.Prices prices(String input, String output) {
		var prices = new AnthropicConfiguration.Prices();
		prices.setInputPerMillion(new BigDecimal(input));
		prices.setOutputPerMillion(new BigDecimal(output));
		return prices;
	}
}
