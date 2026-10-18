package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StandardValue;

/**
 * Runs the shared assembly cases. The Angular test suite runs the same file against
 * {@code assembly.ts} (docs/adr/0006-template-is-an-assembly-of-parts.md).
 */
class AssemblyTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	record CaseContext(double width, double height, double depth, int shelfCount, List<Double> dividers,
			Map<StandardValue, Double> standard, Map<MaterialRole, Double> thicknesses) {
		Assembly.Context context() {
			return new Assembly.Context(width, height, depth, shelfCount, dividers, standard,
					material -> thicknesses.getOrDefault(material.role(), 0d));
		}
	}

	record ExpectedPart(String id, PartRole role, List<Double> box) {
	}

	record ExpectedHardware(MaterialRole role, int quantity) {
	}

	record Case(String name, TemplateAssembly assembly, CaseContext context, List<ExpectedPart> parts,
			List<ExpectedHardware> hardware, List<Assembly.Violation> violations) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() throws IOException {
		try (var input = new ClassPathResource("template-assembly/cases.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<Case>>() {
			}).stream();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void sharedCase(Case testCase) {
		var result = Assembly.assemble(testCase.assembly(), testCase.context().context());

		var parts = result.parts().stream()
				.map(part -> new ExpectedPart(part.id(), part.role(), List.of(round(part.box().x0()),
						round(part.box().x1()), round(part.box().y0()), round(part.box().y1()), round(part.box().z0()),
						round(part.box().z1()))))
				.toList();
		var hardware = result.hardware().stream()
				.map(item -> new ExpectedHardware(item.material().role(), item.quantity()))
				.toList();
		assertThat(result.violations()).as("violations").containsExactlyElementsOf(testCase.violations());
		assertThat(parts).as("parts").containsExactlyElementsOf(testCase.parts());
		assertThat(hardware).as("hardware").containsExactlyElementsOf(testCase.hardware());
	}

	static double round(double millimetres) {
		return Math.round(millimetres * 10) / 10d;
	}
}
