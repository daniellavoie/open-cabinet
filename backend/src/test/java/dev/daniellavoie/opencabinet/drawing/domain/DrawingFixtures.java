package dev.daniellavoie.opencabinet.drawing.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.opencabinet.common.domain.Language;
import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.ApplianceKind;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.OpeningKind;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.shop.domain.model.Units;

/**
 * An L-shaped kitchen with a window, a fridge, a dishwasher, base and wall runs and a pantry.
 */
public final class DrawingFixtures {
	public static final Map<String, CabinetTemplate> TEMPLATES = templates();

	private DrawingFixtures() {
	}

	public static Design kitchen() {
		return Design.builder()
				.walls(List.of(
						Wall.builder().id("w1").lengthMm(3657.6).heightMm(2438.4).angleToNextDegrees(90).build(),
						Wall.builder().id("w2").lengthMm(3048).heightMm(2438.4).angleToNextDegrees(90).build()))
				.cabinets(List.of(
						cabinet("sink", "BASE_SINK", "w1", 1219.2, 914.4, 876.3, 609.6, 0),
						cabinet("drawers", "BASE_DRAWERS", "w1", 762, 457.2, 876.3, 609.6, 0),
						cabinet("doors", "BASE_DOOR", "w1", 2133.6, 457.2, 876.3, 609.6, 0).toBuilder()
								.dividerPositionsMm(List.of(228.6)).build(),
						cabinet("corner", "BASE_BLIND_CORNER", "w1", 2590.8, 1066.8, 876.3, 609.6, 0),
						cabinet("upper1", "WALL_DOOR", "w1", 2133.6, 914.4, 762, 304.8, 1371.6).toBuilder()
								.crown(true).build(),
						cabinet("pantry", "TALL_PANTRY", "w2", 2438.4, 609.6, 2286, 609.6, 0).toBuilder()
								.finishedRight(true).build(),
						cabinet("base2", "BASE_DOOR", "w2", 635, 457.2, 876.3, 609.6, 0)))
				.openings(List.of(Opening.builder().id("o1").kind(OpeningKind.WINDOW).wallId("w1").offsetMm(1219.2)
						.widthMm(914.4).elevationMm(1066.8).heightMm(1066.8).build()))
				.appliances(List.of(
						Appliance.builder().id("a1").kind(ApplianceKind.FRIDGE).wallId("w1").offsetMm(0).widthMm(762)
								.heightMm(1778).depthMm(762).build(),
						Appliance.builder().id("a2").kind(ApplianceKind.DISHWASHER).wallId("w2").offsetMm(1092.2)
								.widthMm(609.6).heightMm(876.3).depthMm(609.6).build()))
				.fillers(List.of(Filler.builder().id("f1").wallId("w2").offsetMm(1701.8).widthMm(76.2).heightMm(774.7)
						.depthMm(609.6).elevationMm(101.6).build()))
				.build();
	}

	public static DrawingContext context(Language language) {
		return DrawingContext.of(kitchen(), TEMPLATES, 101.6, 3, Units.IMPERIAL, language);
	}

	private static Cabinet cabinet(String id, String template, String wall, double offset, double width,
			double height, double depth, double elevation) {
		return Cabinet.builder().id(id).templateCode(template).wallId(wall).offsetMm(offset).widthMm(width)
				.heightMm(height).depthMm(depth).elevationMm(elevation).shelfCount(1).build();
	}

	private static Map<String, CabinetTemplate> templates() {
		try (var input = new ClassPathResource("cabinet-templates.json").getInputStream()) {
			return new ObjectMapper().readValue(input, new TypeReference<List<CabinetTemplate>>() {
			}).stream().collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity()));
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}
