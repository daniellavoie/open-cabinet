package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;

/**
 * Runs the shared Construction Rules cases. The Angular test suite runs the same file against the
 * TypeScript implementation (docs/adr/0002-construction-rules-run-in-browser-and-server.md).
 */
class ConstructionRulesTest {
	private static final ObjectMapper OBJECT_MAPPER = JsonConfiguration.configure(JsonMapper.builder()).build();

	record Case(String name, Design design, List<Violation> violations) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() throws IOException {
		try (var input = new ClassPathResource("construction-rules/cases.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<Case>>() {
			}).stream();
		}
	}

	static List<CabinetTemplate> templates() throws IOException {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void sharedCase(Case testCase) throws IOException {
		assertThat(ConstructionRules.check(testCase.design(), templates()))
				.containsExactlyElementsOf(testCase.violations());
	}
}
