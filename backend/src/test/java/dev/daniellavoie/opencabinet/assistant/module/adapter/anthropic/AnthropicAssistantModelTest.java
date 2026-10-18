package dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ModelMessage;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.Speaker;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolCall;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolResult;
import dev.daniellavoie.opencabinet.assistant.domain.model.ModelRequest.ToolSpec;
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
	void asksTheConfiguredModelWithTheKeyAndCountsTheTokens() throws Exception {
		var requests = new ArrayList<String>();
		var keys = new ArrayList<String>();
		var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", exchange -> {
			keys.add(exchange.getRequestHeaders().getFirst("x-api-key"));
			requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			var reply = """
					{"id": "msg_1", "type": "message", "role": "assistant", "model": "sorter",
					 "content": [{"type": "text", "text": "ready"}], "stop_reason": "end_turn", "stop_sequence": null,
					 "usage": {"input_tokens": 2000, "output_tokens": 500}}
					""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, reply.length);
			exchange.getResponseBody().write(reply);
			exchange.close();
		});
		server.start();
		try {
			var configuration = configuration("test-key");
			configuration.setBaseUrl("http://localhost:" + server.getAddress().getPort());
			var model = new AnthropicAssistantModel(configuration);

			var reply = model.complete(ModelRequest.builder()
					.tier(ModelTier.SORTER)
					.purpose("CHECK")
					.instructions("Reply with one word.")
					.messages(List.of(new ModelMessage(Speaker.MEMBER, "Are you there?")))
					.maxTokens(16)
					.build());

			assertThat(model.isConfigured()).isTrue();
			assertThat(keys).containsExactly("test-key");
			assertThat(requests.get(0)).contains("\"model\":\"sorter\"", "\"max_tokens\":16", "Reply with one word.",
					"Are you there?");
			assertThat(reply.getText()).isEqualTo("ready");
			assertThat(reply.getModel()).isEqualTo("sorter");
			assertThat(reply.getInputTokens()).isEqualTo(2000);
			assertThat(reply.getOutputTokens()).isEqualTo(500);
			assertThat(reply.getCostUsd()).isEqualByComparingTo("0.0045");
		} finally {
			server.stop(0);
		}
	}

	@Test
	void returnsToolCallsWithoutRunningThemAndSendsTheirResultsBack() throws Exception {
		var requests = new ArrayList<String>();
		var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", exchange -> {
			requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			var reply = (requests.size() == 1 ? """
					{"id": "msg_1", "type": "message", "role": "assistant", "model": "builder",
					 "content": [{"type": "text", "text": "Je place le caisson."},
					             {"type": "tool_use", "id": "call_1", "name": "remove_cabinet", "input": {"cabinetId": "c1"}}],
					 "stop_reason": "tool_use", "stop_sequence": null, "usage": {"input_tokens": 100, "output_tokens": 20}}
					""" : """
					{"id": "msg_2", "type": "message", "role": "assistant", "model": "builder",
					 "content": [{"type": "text", "text": "C'est fait."}], "stop_reason": "end_turn", "stop_sequence": null,
					 "usage": {"input_tokens": 150, "output_tokens": 5}}
					""").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, reply.length);
			exchange.getResponseBody().write(reply);
			exchange.close();
		});
		server.start();
		try {
			var configuration = configuration("test-key");
			configuration.setBaseUrl("http://localhost:" + server.getAddress().getPort());
			var model = new AnthropicAssistantModel(configuration);
			var tool = new ToolSpec("remove_cabinet", "Removes a Cabinet.",
					"{\"type\": \"object\", \"properties\": {\"cabinetId\": {\"type\": \"string\"}}, \"required\": [\"cabinetId\"]}");
			var request = ModelRequest.builder()
					.tier(ModelTier.BUILDER)
					.purpose("PROPOSAL")
					.instructions("Edit the design.")
					.messages(List.of(new ModelMessage(Speaker.MEMBER, "Enlève le caisson.")))
					.tools(List.of(tool))
					.maxTokens(1000)
					.build();

			var first = model.complete(request);

			assertThat(first.getText()).isEqualTo("Je place le caisson.");
			assertThat(first.getToolCalls()).containsExactly(new ToolCall("call_1", "remove_cabinet", "{\"cabinetId\":\"c1\"}"));
			assertThat(requests.get(0)).contains("\"tools\"", "\"name\":\"remove_cabinet\"", "Removes a Cabinet.");

			var second = model.complete(request.toBuilder()
					.messages(List.of(new ModelMessage(Speaker.MEMBER, "Enlève le caisson."),
							ModelMessage.assistant(first.getText(), first.getToolCalls()),
							ModelMessage.tool(new ToolResult("call_1", "remove_cabinet", "{\"ok\": true}"))))
					.build());

			assertThat(second.getText()).isEqualTo("C'est fait.");
			assertThat(second.getToolCalls()).isEmpty();
			assertThat(requests.get(1)).contains("\"tool_use\"", "\"tool_result\"", "\"tool_use_id\":\"call_1\"");
		} finally {
			server.stop(0);
		}
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
