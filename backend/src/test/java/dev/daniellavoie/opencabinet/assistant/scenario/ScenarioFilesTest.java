package dev.daniellavoie.opencabinet.assistant.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dev.daniellavoie.opencabinet.project.domain.ConstructionRules;
import dev.daniellavoie.opencabinet.project.domain.DesignGuidelines;
import dev.daniellavoie.opencabinet.project.domain.model.Remark;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * Every scenario can run: its file reads, its attachments are there, its starting design passes the
 * Construction Rules and it expects something. Real runs are rare; a broken scenario should not wait
 * for one.
 */
class ScenarioFilesTest {
	static Stream<String> names() throws IOException {
		return Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath:" + ScenarioHarness.SCENARIOS + "*.json"))
				.map(resource -> resource.getFilename().replaceFirst("\\.json$", ""));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("names")
	void canRun(String name) {
		var scenario = ScenarioHarness.scenario(name);

		assertThat(scenario.name()).isEqualTo(name);
		assertThat(scenario.language()).isIn("fr", "en");
		assertThat(scenario.messages()).isNotEmpty();
		assertThat(scenario.expect()).isNotNull();
		assertThat(ConstructionRules.check(scenario.design(), ScenarioHarness.templates())).isEmpty();
		scenario.messages().stream().flatMap(message -> message.attachments().stream())
				.forEach(file -> assertThat(new ClassPathResource(ScenarioHarness.SCENARIOS + file).exists()).as(file).isTrue());
	}

	/**
	 * The reorganizing scenario has something to fix: it starts with the remarks it was built for.
	 */
	@org.junit.jupiter.api.Test
	void theMuddledKitchenStartsWithRemarks() {
		var codes = DesignGuidelines.check(ScenarioHarness.scenario("reorganize-kitchen").design(), ScenarioHarness.templates(),
				Units.IMPERIAL).stream().map(Remark::getCode).toList();

		assertThat(codes).contains(DesignGuidelines.GAP, DesignGuidelines.UPPER_ALIGNMENT, DesignGuidelines.RANGE_CLEARANCE,
				DesignGuidelines.DISHWASHER_NOT_BESIDE_SINK, DesignGuidelines.LANDING_RANGE);
	}
}
