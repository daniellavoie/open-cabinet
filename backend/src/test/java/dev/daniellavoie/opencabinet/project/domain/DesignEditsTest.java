package dev.daniellavoie.opencabinet.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.project.domain.DesignEdits.ItemChanges;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;

/**
 * Runs the shared design edit cases. The Angular test suite runs the same file against
 * {@code design-edits.ts}: the Design Assistant's steps replay these edits on the server
 * (docs/adr/0004-design-assistant-proposes-member-accepts.md).
 */
class DesignEditsTest {
	private static final ObjectMapper OBJECT_MAPPER = JsonConfiguration.configure(JsonMapper.builder()).build();

	private static final Map<String, BiFunction<Design, JsonNode, Design>> ACTIONS = Map.ofEntries(
			Map.entry("addWall", (design, args) -> DesignEdits.addWall(design, args.get("id").asString())),
			Map.entry("updateWall", (design, args) -> DesignEdits.updateWall(design, args.get("wallId").asString(),
					wall -> merge(wall, args.get("changes"), Wall.class))),
			Map.entry("removeWall", (design, args) -> DesignEdits.removeWall(design, args.get("wallId").asString())),
			Map.entry("addFiller", (design, args) -> DesignEdits.addFiller(design, args.get("wallId").asString(),
					args.get("id").asString())),
			Map.entry("addOpening", (design, args) -> DesignEdits.addOpening(design,
					OpeningKind.valueOf(args.get("kind").asString()), args.get("wallId").asString(), args.get("id").asString())),
			Map.entry("addAppliance", (design, args) -> DesignEdits.addAppliance(design,
					ApplianceKind.valueOf(args.get("kind").asString()), args.get("wallId").asString(), args.get("id").asString())),
			Map.entry("updateItem", (design, args) -> DesignEdits.updateItem(design, args.get("itemId").asString(),
					OBJECT_MAPPER.treeToValue(args.get("changes"), ItemChanges.class))),
			Map.entry("removeItem", (design, args) -> DesignEdits.removeItem(design, args.get("itemId").asString())),
			Map.entry("addCabinet", (design, args) -> {
				var template = templates().stream()
						.filter(item -> item.getCode().equals(args.get("templateCode").asString())
								&& item.getVersion() == args.get("templateVersion").asInt())
						.findFirst()
						.orElseThrow();
				return DesignEdits.addCabinet(design, template, args.get("wallId").asString(), templates(), args.get("id").asString());
			}),
			Map.entry("updateCabinet", (design, args) -> DesignEdits.updateCabinet(design, args.get("cabinetId").asString(),
					cabinet -> merge(cabinet, args.get("changes"), Cabinet.class))),
			Map.entry("addDivider", (design, args) -> DesignEdits.addDivider(design, args.get("cabinetId").asString())),
			Map.entry("moveDivider", (design, args) -> DesignEdits.moveDivider(design, args.get("cabinetId").asString(),
					args.get("index").asInt(), args.get("positionMm").asDouble())),
			Map.entry("removeDivider", (design, args) -> DesignEdits.removeDivider(design, args.get("cabinetId").asString(),
					args.get("index").asInt())),
			Map.entry("removeCabinet", (design, args) -> DesignEdits.removeCabinet(design, args.get("cabinetId").asString())));

	record Case(String name, String action, JsonNode args, Design design, Design expected) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() throws IOException {
		try (var input = new ClassPathResource("design-edits/cases.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<Case>>() {
			}).stream();
		}
	}

	private static List<CabinetTemplate> templates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static <T> T merge(T value, JsonNode changes, Class<T> type) {
		var tree = (ObjectNode) OBJECT_MAPPER.valueToTree(value);
		tree.setAll((ObjectNode) changes);
		return OBJECT_MAPPER.treeToValue(tree, type);
	}

	@Test
	void coversEveryEdit() throws IOException {
		assertThat(cases().map(Case::action).distinct()).containsExactlyInAnyOrderElementsOf(ACTIONS.keySet());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void sharedCase(Case testCase) {
		assertThat(ACTIONS.get(testCase.action()).apply(testCase.design(), testCase.args()))
				.isEqualTo(testCase.expected());
	}
}
