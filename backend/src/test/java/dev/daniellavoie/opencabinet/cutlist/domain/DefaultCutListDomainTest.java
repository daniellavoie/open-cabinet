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
import dev.daniellavoie.opencabinet.client.domain.port.api.ClientDomain;
import dev.daniellavoie.opencabinet.cutlist.domain.model.BomLine;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.FrontSource;
import dev.daniellavoie.opencabinet.project.domain.model.Project;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.shop.domain.port.backend.ShopRepository;

class DefaultCutListDomainTest {
	private final ProjectDomain projectDomain = mock(ProjectDomain.class);
	private final DefaultCutListDomain domain = new DefaultCutListDomain(mock(MemberDomain.class), projectDomain,
			mock(CatalogDomain.class), mock(ClientDomain.class), mock(ShopRepository.class));

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

	@Test
	void orderedFrontsLeaveTheCutListForTheBillOfMaterials() {
		when(projectDomain.templates()).thenReturn(List.of(BASE_DOOR, BASE_DRAWERS));

		var project = Project.builder()
				.id(UUID.randomUUID())
				.design(Design.builder()
						.walls(List.of(Wall.builder().id("w1").lengthMm(3000).heightMm(2400).build()))
						.cabinets(List.of(
								cabinet("doors", "w1", "BASE_DOOR", 0, 0, 1).toBuilder()
										.frontSource(FrontSource.ORDERED).build(),
								cabinet("drawers", "w1", "BASE_DRAWERS", 457.2, 0, 0).toBuilder()
										.frontSource(FrontSource.ORDERED).orderedFrontId(SLAB.getId()).build()))
						.build())
				.build();

		var output = domain.output(project, STANDARD, CATALOG);

		assertThat(output.getCutList()).noneMatch(row -> row.getRole() == PartRole.DOOR
				|| row.getRole() == PartRole.DRAWER_FRONT);
		assertThat(output.getNesting()).noneMatch(nesting -> nesting.getMaterialId().equals(FRONT.getId()));
		var door = output.getOrderedFronts().stream().filter(row -> row.getRole() == PartRole.DOOR).findFirst()
				.orElseThrow();
		assertThat(door.getMaterialName()).isEqualTo("Shaker");
		assertThat(door.getHeightMm()).isEqualTo(PartGenerator.round(876.3 - 101.6 - 3));
		assertThat(door.getWidthMm()).isEqualTo(PartGenerator.round(457.2 - 3));
		assertThat(output.getOrderedFronts()).filteredOn(row -> row.getRole() == PartRole.DRAWER_FRONT)
				.allSatisfy(row -> assertThat(row.getMaterialName()).isEqualTo("Slab"))
				.extracting(row -> row.getQuantity()).containsOnly(1, 2);

		var shaker = output.getBillOfMaterials().stream()
				.filter(line -> line.getMaterialId().equals(SHAKER.getId())).findFirst().orElseThrow();
		var area = Math.ceil(PartGenerator.round(876.3 - 101.6 - 3) * PartGenerator.round(457.2 - 3) / 1_000_000 * 100) / 100;
		assertThat(shaker.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(area));
		assertThat(shaker.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(area).multiply(new BigDecimal("300")));
		assertThat(output.getBillOfMaterials()).anyMatch(line -> line.getMaterialId().equals(SLAB.getId()));
		assertThat(output.getBillOfMaterials()).as("hinges and slides are still bought")
				.anyMatch(line -> line.getMaterialId().equals(HINGE.getId()))
				.anyMatch(line -> line.getMaterialId().equals(SLIDE.getId()));
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
