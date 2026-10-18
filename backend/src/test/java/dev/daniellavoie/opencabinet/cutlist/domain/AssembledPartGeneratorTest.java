package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CARCASS;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CARCASS_BANDING;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.CATALOG;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.FRONT;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.HINGE;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.SHAKER;
import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.STANDARD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.common.config.JsonConfiguration;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.FrontSource;
import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly;

/**
 * An assembled template's Cut List: the base cabinet of {@code template-assembly/cases.json} built
 * with the test catalog.
 */
class AssembledPartGeneratorTest {
	private static final Map<java.util.UUID, Material> MATERIALS = CATALOG.stream()
			.collect(Collectors.toMap(Material::getId, Function.identity()));

	@Test
	void panelsAreCutWithTheirGrainAndBandedFaces() throws IOException {
		var set = PartGenerator.generate(cabinet(null), template(), "C1", STANDARD, MATERIALS);

		assertThat(set.parts()).extracting(Part::getRole).containsExactly(PartRole.SIDE, PartRole.SIDE, PartRole.BACK,
				PartRole.BOTTOM, PartRole.STRETCHER, PartRole.SHELF, PartRole.DRAWER_FRONT, PartRole.DOOR);
		var side = set.parts().get(0);
		// Up the side (grain), then its depth, its front edge banded.
		assertThat(side.getMaterialId()).isEqualTo(CARCASS.getId());
		assertThat(side.getLengthMm()).isEqualTo(774.7);
		assertThat(side.getWidthMm()).isEqualTo(609.6 - FRONT.getThicknessMm());
		assertThat(side.getBandedLengthEdges()).isEqualTo(1);
		assertThat(side.getBandedWidthEdges()).isZero();
		assertThat(side.getEdgeBandingId()).isEqualTo(CARCASS_BANDING.getId());

		var door = set.parts().get(7);
		assertThat(door.getMaterialId()).isEqualTo(FRONT.getId());
		assertThat(door.getBandedLengthEdges() + door.getBandedWidthEdges()).isEqualTo(4);
		assertThat(set.hardware()).containsEntry(HINGE.getId(), 2);
	}

	@Test
	void frontsAreOrderedWhenTheCabinetOrdersThem() throws IOException {
		var set = PartGenerator.generate(cabinet(FrontSource.ORDERED), template(), "C1", STANDARD, MATERIALS);

		assertThat(set.parts()).filteredOn(Part::isOrdered).extracting(Part::getRole)
				.containsExactly(PartRole.DRAWER_FRONT, PartRole.DOOR);
		assertThat(set.parts()).filteredOn(Part::isOrdered)
				.allSatisfy(part -> assertThat(part.getMaterialId()).isEqualTo(SHAKER.getId()));
	}

	private static Cabinet cabinet(FrontSource frontSource) {
		return Cabinet.builder().id("c1").templateCode("TEST_BASE").templateVersion(1).wallId("w1").offsetMm(0)
				.widthMm(457.2).heightMm(876.3).depthMm(609.6).elevationMm(0).shelfCount(1).frontSource(frontSource)
				.build();
	}

	private static CabinetTemplate template() throws IOException {
		try (var input = new ClassPathResource("template-assembly/cases.json").getInputStream()) {
			var cases = JsonConfiguration.configure(JsonMapper.builder()).build().readTree(input);
			var assembly = JsonConfiguration.configure(JsonMapper.builder()).build().treeToValue(cases.get(0).get("assembly"), TemplateAssembly.class);
			return CabinetTemplate.builder().code("TEST_BASE").version(1).type(CabinetType.BASE).maxShelfCount(4)
					.frontRows(List.of()).assembly(assembly).build();
		}
	}
}
