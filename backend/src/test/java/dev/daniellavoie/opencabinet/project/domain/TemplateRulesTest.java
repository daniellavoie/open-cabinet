package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StandardValue;

/**
 * Runs the shared template rules cases, which the Angular test suite runs against
 * {@code template-rules.ts}.
 */
class TemplateRulesTest {
	private static final ObjectMapper OBJECT_MAPPER = JsonConfiguration.configure(JsonMapper.builder()).build();

	record CaseShop(Map<StandardValue, Double> standard, Map<MaterialRole, Double> thicknesses,
			Map<MaterialRole, TemplateRules.Sheet> sheets, double trimMm) {
		TemplateRules.Shop shop() {
			return new TemplateRules.Shop(standard, material -> thicknesses.getOrDefault(material.role(), 0d),
					material -> sheets.get(material.role()), trimMm);
		}
	}

	record Case(String name, CabinetTemplate template, CaseShop shop, List<TemplateRules.Violation> violations) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() throws IOException {
		try (var input = new ClassPathResource("template-assembly/rules.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<Case>>() {
			}).stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void sharedCase(Case testCase) {
		assertThat(TemplateRules.check(testCase.template(), testCase.shop().shop()))
				.containsExactlyElementsOf(testCase.violations());
	}
}
