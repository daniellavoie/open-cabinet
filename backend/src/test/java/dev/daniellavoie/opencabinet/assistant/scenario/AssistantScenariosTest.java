package dev.daniellavoie.opencabinet.assistant.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic.AnthropicAssistantModel;
import dev.daniellavoie.opencabinet.assistant.module.adapter.anthropic.AnthropicConfiguration;

/**
 * The scenario suite against the real model, on demand only, to compare models and to record
 * their replies for {@link RecordedScenariosTest}:
 *
 * <pre>
 * ANTHROPIC_API_KEY=... ./mvnw -pl backend test -Pscenarios \
 *     -Dscenario.models=claude-sonnet-5-5,claude-opus-5-5 [-Dscenario.names=galley-from-sketch] [-Dscenario.record=true]
 * </pre>
 *
 * The report is written to {@code target/assistant-scenarios/report.md} and the recordings next to
 * it; {@code scenario.record=true} also writes them to the test resources, to be committed.
 */
@Tag("scenarios")
class AssistantScenariosTest {
	private static final Path OUTPUT = Path.of("target/assistant-scenarios");
	private static final Path RECORDINGS = Path.of("src/test/resources/assistant-scenarios/recordings");

	@Test
	void runEveryScenarioAgainstEachModel() throws IOException {
		var apiKey = System.getenv("ANTHROPIC_API_KEY");
		assumeTrue(apiKey != null && !apiKey.isBlank(), "ANTHROPIC_API_KEY is not set");
		var models = Arrays.asList(System.getProperty("scenario.models", "claude-sonnet-5-5").split(","));
		var names = scenarioNames();
		var record = Boolean.getBoolean("scenario.record");
		Files.createDirectories(OUTPUT.resolve("recordings"));

		var report = new StringBuilder("# Design Assistant scenarios\n\n")
				.append("| Scenario | Model | Checks | Steps | Rounds | Input tokens | Output tokens | Cost (US$) | Seconds |\n")
				.append("|---|---|---|---|---|---|---|---|---|\n");
		var failures = new ArrayList<String>();
		var details = new StringBuilder();
		for (var model : models) {
			var anthropic = new AnthropicAssistantModel(configuration(apiKey, model.strip()));
			for (var name : names) {
				var scenario = ScenarioHarness.scenario(name);
				var recording = RecordingModel.recording(anthropic);
				var run = ScenarioHarness.run(scenario, recording);
				var passed = run.checks().stream().filter(ScenarioChecks.Check::passed).count();
				var proposal = run.view().getProposal();
				report.append("| ").append(name).append(" | ").append(model).append(" | ").append(passed).append("/")
						.append(run.checks().size()).append(" | ").append(proposal == null ? 0 : proposal.getSteps().size())
						.append(" | ").append(run.rounds()).append(" | ").append(run.inputTokens()).append(" | ")
						.append(run.outputTokens()).append(" | ").append(run.costUsd().setScale(4, RoundingMode.UP)).append(" | ")
						.append(run.duration().toSeconds()).append(" |\n");
				details.append("\n## ").append(name).append(" — ").append(model).append("\n\n");
				run.checks().forEach(check -> details.append("- ").append(check.passed() ? "✅ " : "❌ ").append(check.name())
						.append(check.passed() || check.detail().isBlank() ? "" : " (" + check.detail() + ")").append("\n"));
				if (proposal != null) {
					details.append("\nSummary: ").append(proposal.getSummary()).append("\n");
					proposal.getOpenQuestions().forEach(question -> details.append("- Question: ").append(question).append("\n"));
					proposal.getAssumptions().forEach(assumption -> details.append("- Assumption: ").append(assumption).append("\n"));
				}
				run.view().getMessages().stream().filter(message -> message.getText() != null)
						.reduce((first, last) -> last)
						.ifPresent(answer -> details.append("\nAnswer: ").append(answer.getText()).append("\n"));
				if (!run.passed()) {
					failures.add(name + " with " + model);
				}

				var file = name + "." + model.strip() + ".json";
				var json = ScenarioHarness.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(new Recording(name, model.strip(),
						"Recorded from " + model.strip() + " by AssistantScenariosTest.", recording.replies()));
				Files.writeString(OUTPUT.resolve("recordings").resolve(file), json);
				if (record) {
					Files.createDirectories(RECORDINGS);
					Files.writeString(RECORDINGS.resolve(file), json);
				}
			}
		}
		Files.writeString(OUTPUT.resolve("report.md"), report.append(details).toString());

		assertThat(failures).as("scenarios with failed checks; see target/assistant-scenarios/report.md").isEmpty();
	}

	private static List<String> scenarioNames() throws IOException {
		var chosen = System.getProperty("scenario.names");
		if (chosen != null && !chosen.isBlank()) {
			return Arrays.stream(chosen.split(",")).map(String::strip).toList();
		}
		return Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath:" + ScenarioHarness.SCENARIOS + "*.json"))
				.map(resource -> resource.getFilename().replaceFirst("\\.json$", ""))
				.sorted()
				.toList();
	}

	/**
	 * The adapter as the app configures it, with the prices from {@code application.properties}.
	 */
	private static AnthropicConfiguration configuration(String apiKey, String model) throws IOException {
		var properties = new Properties();
		try (var input = new ClassPathResource("application.properties").getInputStream()) {
			properties.load(input);
		}
		var pattern = Pattern.compile("open-cabinet\\.assistant\\.anthropic\\.prices\\.\\[(.+)]\\.(input|output)-per-million");
		Map<String, AnthropicConfiguration.Prices> prices = properties.stringPropertyNames().stream()
				.map(pattern::matcher)
				.filter(java.util.regex.Matcher::matches)
				.collect(Collectors.groupingBy(matcher -> matcher.group(1), Collectors.collectingAndThen(Collectors.toList(), matchers -> {
					var price = new AnthropicConfiguration.Prices();
					for (var matcher : matchers) {
						var value = new BigDecimal(properties.getProperty(matcher.group(0)));
						if (matcher.group(2).equals("input")) {
							price.setInputPerMillion(value);
						} else {
							price.setOutputPerMillion(value);
						}
					}
					return price;
				})));
		var configuration = new AnthropicConfiguration();
		configuration.setApiKey(apiKey);
		configuration.setBuilderModel(model);
		configuration.setSorterModel(properties.getProperty("open-cabinet.assistant.anthropic.sorter-model"));
		configuration.setPrices(prices);
		return configuration;
	}
}
