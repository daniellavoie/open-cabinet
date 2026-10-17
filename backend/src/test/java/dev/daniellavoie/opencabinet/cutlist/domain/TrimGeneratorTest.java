package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Filler;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;

class TrimGeneratorTest {
	private static final Map<UUID, Material> MATERIALS = CATALOG.stream()
			.collect(Collectors.toMap(Material::getId, Function.identity()));
	private static final Map<String, CabinetTemplate> TEMPLATES = List.of(BASE_DOOR, WALL_DOOR).stream()
			.collect(Collectors.toMap(CabinetTemplate::key, Function.identity()));

	@Test
	void toeKicksFollowRunsOfFloorCabinets() {
		var design = design(List.of(
				base("a", 0), base("b", 457.2), base("c", 2000),
				wall("w", 0, false)), List.of());

		var set = TrimGenerator.generate(design, Map.of("a", "C1", "b", "C2", "c", "C3", "w", "C4"), TEMPLATES,
				STANDARD, MATERIALS);

		var toeKicks = set.parts().stream().filter(part -> part.getRole() == PartRole.TOE_KICK).toList();
		assertThat(toeKicks).extracting(Part::getCabinetLabel).containsExactly("C1–C2", "C3");
		assertThat(toeKicks.get(0).getLengthMm()).isEqualTo(914.4);
		assertThat(toeKicks.get(0).getWidthMm()).isEqualTo(STANDARD.getToeKickHeightMm());
		assertThat(set.mouldingMeters()).isEmpty();
	}

	@Test
	void longRunsAreSplitToFitASheet() {
		var cabinets = java.util.stream.IntStream.range(0, 7).mapToObj(index -> base("c" + index, index * 457.2))
				.toList();

		var set = TrimGenerator.generate(design(cabinets, List.of()),
				cabinets.stream().collect(Collectors.toMap(Cabinet::getId, Cabinet::getId)), TEMPLATES, STANDARD,
				MATERIALS);

		assertThat(set.parts()).filteredOn(part -> part.getRole() == PartRole.TOE_KICK)
				.hasSize(2)
				.allSatisfy(part -> assertThat(part.getLengthMm()).isEqualTo(PartGenerator.round(7 * 457.2 / 2)));
	}

	@Test
	void crownMouldingRunsAlongWallCabinetsWithReturnsAtOpenEnds() {
		var design = design(List.of(wall("a", 0, true), wall("b", 914.4, true), wall("c", 2500, false)), List.of());

		var set = TrimGenerator.generate(design, Map.of("a", "C1", "b", "C2", "c", "C3"), TEMPLATES, STANDARD,
				MATERIALS);

		var expected = BigDecimal.valueOf((2 * 914.4 + 304.8) / 1000 * 1.10).setScale(1, java.math.RoundingMode.CEILING);
		assertThat(set.mouldingMeters()).containsExactly(Map.entry(CROWN.getId(), expected));
	}

	@Test
	void fillersAreCutFromFrontMaterialInReadingOrder() {
		var design = design(List.of(), List.of(
				Filler.builder().id("f2").wallId("w1").offsetMm(900).widthMm(50.8).heightMm(774.7).depthMm(609.6)
						.elevationMm(101.6).build(),
				Filler.builder().id("f1").wallId("w1").offsetMm(0).widthMm(76.2).heightMm(774.7).depthMm(609.6)
						.elevationMm(101.6).build()));

		var set = TrimGenerator.generate(design, Map.of(), TEMPLATES, STANDARD, MATERIALS);

		assertThat(set.parts()).extracting(Part::getRole, Part::getCabinetLabel, Part::getWidthMm,
				Part::getMaterialId)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(PartRole.FILLER, "F1", 76.2, FRONT.getId()),
						org.assertj.core.groups.Tuple.tuple(PartRole.FILLER, "F2", 50.8, FRONT.getId()));
	}

	private static Design design(List<Cabinet> cabinets, List<Filler> fillers) {
		return Design.builder()
				.walls(List.of(Wall.builder().id("w1").lengthMm(3657.6).heightMm(2438.4).angleToNextDegrees(90).build()))
				.cabinets(cabinets)
				.fillers(fillers)
				.build();
	}

	private static Cabinet base(String id, double offset) {
		return Cabinet.builder().id(id).templateCode("BASE_DOOR").templateVersion(1).wallId("w1").offsetMm(offset).widthMm(457.2)
				.heightMm(876.3).depthMm(609.6).build();
	}

	private static Cabinet wall(String id, double offset, boolean crown) {
		return Cabinet.builder().id(id).templateCode("WALL_DOOR").templateVersion(1).wallId("w1").offsetMm(offset).widthMm(914.4)
				.heightMm(762).depthMm(304.8).elevationMm(1371.6).crown(crown).build();
	}
}
