package dev.daniellavoie.opencabinet.assistant.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.FrontSource;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;

class DesignToolsTest {
	private static final ObjectMapper OBJECT_MAPPER = JsonConfiguration.configure(JsonMapper.builder()).build();
	private static final Path EDITOR_ACTIONS = Path.of("src/main/webapp/src/app/project/design-edits.ts");

	/**
	 * ADR 0004: the assistant proposes only what the editor can do, and can do all of it.
	 */
	@Test
	void everyEditorActionHasATool() throws IOException {
		var exported = Pattern.compile("^export function (\\w+)", Pattern.MULTILINE)
				.matcher(Files.readString(EDITOR_ACTIONS))
				.results()
				.map(match -> match.group(1))
				.filter(name -> !name.equals("newId"))
				.collect(Collectors.toSet());
		var covered = DesignTools.ALL.stream().flatMap(tool -> tool.editorActions().stream()).collect(Collectors.toSet());

		assertThat(exported).isNotEmpty();
		assertThat(covered).as("a tool for every editor action in design-edits.ts").containsAll(exported);
		assertThat(exported).as("tools perform only editor actions").containsAll(covered);
	}

	@Test
	void schemasAreObjectsRequiringOnlyTheirProperties() {
		for (var tool : DesignTools.ALL) {
			var schema = OBJECT_MAPPER.readTree(tool.inputSchema());
			assertThat(schema.get("type").asString()).as(tool.name()).isEqualTo("object");
			for (var required : schema.get("required")) {
				assertThat(schema.get("properties").has(required.asString())).as(tool.name() + " " + required).isTrue();
			}
		}
	}

	@Test
	void addsACabinetWithTheIdItIsGivenAndTheSizesAsked() {
		var outcome = DesignTools.BY_NAME.get("add_cabinet").apply(room(), arguments("""
				{"templateCode": "BASE_DOOR", "wallId": "w1", "widthMm": 600, "shelfCount": 2}"""), context("cabinet-1"));

		var cabinet = outcome.design().getCabinets().get(0);
		assertThat(outcome.createdId()).isEqualTo("cabinet-1");
		assertThat(cabinet.getId()).isEqualTo("cabinet-1");
		assertThat(cabinet.getTemplateVersion()).isEqualTo(2);
		assertThat(cabinet.getWidthMm()).isEqualTo(600);
		assertThat(cabinet.getShelfCount()).isEqualTo(2);
		assertThat(cabinet.getHeightMm()).isEqualTo(876.3);
	}

	@Test
	void ordersFrontsByStyleName() {
		var design = DesignTools.BY_NAME.get("add_cabinet").apply(room(), arguments("""
				{"templateCode": "BASE_DOOR", "wallId": "w1"}"""), context("c1")).design();
		var style = UUID.randomUUID();
		var tool = DesignTools.BY_NAME.get("update_cabinet");

		var ordered = tool.apply(design, arguments("""
				{"cabinetId": "c1", "frontSource": "ORDERED", "orderedFrontStyle": "Shaker blanc"}"""),
				context("unused", Map.of("Shaker blanc", style))).design().getCabinets().get(0);

		assertThat(ordered.getFrontSource()).isEqualTo(FrontSource.ORDERED);
		assertThat(ordered.getOrderedFrontId()).isEqualTo(style);
		assertThatThrownBy(() -> tool.apply(design, arguments("""
				{"cabinetId": "c1", "orderedFrontStyle": "Inconnu"}"""), context("unused")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Inconnu");
	}

	@Test
	void refusesWhatIsNotInTheDesign() {
		assertThatThrownBy(() -> DesignTools.BY_NAME.get("remove_cabinet").apply(room(), arguments("{\"cabinetId\": \"nope\"}"),
				context("unused"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nope");
		assertThatThrownBy(() -> DesignTools.BY_NAME.get("add_cabinet").apply(room(), arguments("""
				{"templateCode": "UNKNOWN", "wallId": "w1"}"""), context("unused")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> DesignTools.BY_NAME.get("update_wall").apply(room(), arguments("""
				{"wallId": "w1", "lengthMm": "long"}"""), context("unused")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lengthMm");
	}

	private static Design room() {
		return Design.builder()
				.walls(List.of(Wall.builder().id("w1").lengthMm(3000).heightMm(2400).angleToNextDegrees(90).build()))
				.cabinets(List.of())
				.build();
	}

	private static tools.jackson.databind.JsonNode arguments(String json) {
		return OBJECT_MAPPER.readTree(json);
	}

	private static DesignToolContext context(String newId) {
		return context(newId, Map.of());
	}

	private static DesignToolContext context(String newId, Map<String, UUID> frontStyles) {
		var templates = templates();
		var newest = templates.stream().collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity(),
				(first, second) -> first.getVersion() >= second.getVersion() ? first : second));
		return new DesignToolContext(templates, newest, frontStyles, prefix -> newId);
	}

	private static List<CabinetTemplate> templates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return OBJECT_MAPPER.readValue(input, new TypeReference<List<CabinetTemplate>>() {
			});
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
