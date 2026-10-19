package dev.daniellavoie.opencabinet.assistant.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import tools.jackson.core.type.TypeReference;

/**
 * Replays the recorded model replies of the scenario suite through the Conversation domain, without
 * the model: a change that breaks how the domain takes a real model's tool calls fails here.
 */
class RecordedScenariosTest {
	static Stream<Recording> recordings() throws IOException {
		return Arrays.stream(new PathMatchingResourcePatternResolver()
				.getResources("classpath:" + ScenarioHarness.SCENARIOS + "recordings/*.json"))
				.map(resource -> {
					try (var input = resource.getInputStream()) {
						return ScenarioHarness.JSON.readValue(input, new TypeReference<Recording>() {
						});
					} catch (IOException exception) {
						throw new IllegalStateException(exception);
					}
				});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recordings")
	void replay(Recording recording) {
		var model = RecordingModel.replaying(recording);

		var run = ScenarioHarness.run(ScenarioHarness.scenario(recording.scenario()), model);

		assertThat(run.checks()).allSatisfy(check -> assertThat(check.passed()).as(check.name() + " " + check.detail()).isTrue());
		assertThat(model.exhausted()).as("every recorded reply was asked for").isTrue();
	}
}
