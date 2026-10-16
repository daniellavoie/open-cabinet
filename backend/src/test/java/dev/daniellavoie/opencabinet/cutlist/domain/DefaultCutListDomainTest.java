package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.catalog.domain.port.api.CatalogDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.model.BomLine;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;

class DefaultCutListDomainTest {
	private final ProjectDomain projectDomain = mock(ProjectDomain.class);
	private final DefaultCutListDomain domain = new DefaultCutListDomain(mock(MemberDomain.class), projectDomain,
			mock(CatalogDomain.class), mock(ShopRepository.class));

	@Test
	void outputLabelsCabinetsInReadingOrderAndPricesMaterials() {
		when(projectDomain.templates()).thenReturn(List.of(BASE_DOOR, BASE_DRAWERS, WALL_DOOR));

		var project = Project.builder()
				.id(UUID.randomUUID())
				.design(Design.builder()
						.walls(List.of(
								Wall.builder().id("w1").lengthMm(3000).heightMm(2400).build(),
								Wall.builder().id("w2").lengthMm(3000).heightMm(2400).build()))
						.cabinets(List.of(
								cabinet("late", "w2", "BASE_DOOR", 0, 0, 1),
								cabinet("wall", "w1", "WALL_DOOR", 0, 1371.6, 2),
								cabinet("second", "w1", "BASE_DRAWERS", 457.2, 0, 0),
								cabinet("first", "w1", "BASE_DOOR", 0, 0, 1)))
						.build())
				.build();

		var output = domain.output(project, STANDARD, CATALOG);

		assertThat(output.getCabinetLabels()).containsExactly(
				org.assertj.core.api.Assertions.entry("first", "C1"),
				org.assertj.core.api.Assertions.entry("wall", "C2"),
				org.assertj.core.api.Assertions.entry("second", "C3"),
				org.assertj.core.api.Assertions.entry("late", "C4"));
		assertThat(output.getOperations().getCabinets()).isEqualTo(4);
		assertThat(output.getOperations().getSheets()).isEqualTo(output.getNesting().stream()
				.mapToInt(nesting -> nesting.getSheets().size()).sum());

		var sheetLines = output.getBillOfMaterials().stream().filter(line -> line.getKind() == MaterialKind.SHEET)
				.toList();
		assertThat(sheetLines).extracting(BomLine::getMaterialId)
				.containsExactlyInAnyOrder(CARCASS.getId(), BACK.getId(), FRONT.getId());

		var total = output.getBillOfMaterials().stream().map(BomLine::getTotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		assertThat(output.getMaterialsTotal()).isEqualByComparingTo(total);
		assertThat(output.getCutList()).allSatisfy(row -> assertThat(row.getQuantity()).isPositive());
	}

	private static Cabinet cabinet(String id, String wallId, String template, double offset, double elevation,
			int shelves) {
		var wall = template.startsWith("WALL");
		return Cabinet.builder()
				.id(id)
				.wallId(wallId)
				.templateCode(template)
				.offsetMm(offset)
				.widthMm(457.2)
				.heightMm(wall ? 762 : 876.3)
				.depthMm(wall ? 304.8 : 609.6)
				.elevationMm(elevation)
				.shelfCount(shelves)
				.build();
	}
}
