package dev.daniellavoie.opencabinet.cutlist.domain;

import static dev.daniellavoie.opencabinet.cutlist.domain.CutListFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;

class PartGeneratorTest {
	private static final Map<UUID, Material> MATERIALS = CATALOG.stream()
			.collect(Collectors.toMap(Material::getId, Function.identity()));

	@Test
	void narrowBaseCabinetWithOneDoorAndShelf() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("BASE_DOOR").templateVersion(1).wallId("w1")
				.widthMm(457.2).heightMm(876.3).depthMm(609.6).shelfCount(1)
				.build();

		var set = PartGenerator.generate(cabinet, BASE_DOOR, "C1", STANDARD, MATERIALS);

		var boxHeight = 876.3 - 101.6;
		var carcassDepth = 609.6 - 15.9 - 6.4;
		assertThat(count(set, PartRole.SIDE)).isEqualTo(2);
		assertThat(first(set, PartRole.SIDE).getLengthMm()).isEqualTo(round(boxHeight));
		assertThat(first(set, PartRole.SIDE).getWidthMm()).isEqualTo(round(carcassDepth));
		assertThat(first(set, PartRole.SIDE).getBandedLengthEdges()).isEqualTo(1);
		var bottom = first(set, PartRole.BOTTOM);
		assertThat(bottom.getLengthMm()).as("ungrained parts put their long side as length")
				.isEqualTo(round(carcassDepth));
		assertThat(bottom.getWidthMm()).isEqualTo(round(457.2 - 2 * 15.9));
		assertThat(bottom.getBandedWidthEdges()).as("the front edge follows the turn").isEqualTo(1);
		assertThat(bottom.getBandedLengthEdges()).isZero();
		assertThat(count(set, PartRole.STRETCHER)).isEqualTo(2);
		assertThat(count(set, PartRole.TOP)).isZero();
		assertThat(first(set, PartRole.BACK).getMaterialId()).isEqualTo(BACK.getId());
		assertThat(first(set, PartRole.BACK).getEdgeBandingId()).isNull();
		assertThat(count(set, PartRole.SHELF)).isEqualTo(1);

		var door = first(set, PartRole.DOOR);
		assertThat(count(set, PartRole.DOOR)).isEqualTo(1);
		assertThat(door.getLengthMm()).as("grain runs up the door").isEqualTo(round(boxHeight - 3));
		assertThat(door.getWidthMm()).isEqualTo(round(457.2 - 3));
		assertThat(door.getBandedLengthEdges()).isEqualTo(2);
		assertThat(door.getBandedWidthEdges()).isEqualTo(2);
		assertThat(door.getEdgeBandingId()).isEqualTo(FRONT_BANDING.getId());

		assertThat(set.hardware())
				.containsEntry(HINGE.getId(), 2)
				.containsEntry(SHELF_PIN.getId(), 4)
				.containsEntry(PULL.getId(), 1)
				.containsEntry(LEG.getId(), 4);
	}

	@Test
	void dividersSplitTheShelves() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("WALL_DOOR").templateVersion(1).wallId("w1")
				.widthMm(914.4).heightMm(762).depthMm(304.8).elevationMm(1371.6).shelfCount(2)
				.dividerPositionsMm(List.of(300d))
				.build();

		var set = PartGenerator.generate(cabinet, WALL_DOOR, "C1", STANDARD, MATERIALS);

		var shelfDepth = 304.8 - 15.9 - 6.4 - 25;
		var divider = first(set, PartRole.DIVIDER);
		assertThat(count(set, PartRole.DIVIDER)).isEqualTo(1);
		assertThat(divider.getLengthMm()).isEqualTo(round(762 - 2 * 15.9));
		assertThat(divider.getWidthMm()).isEqualTo(round(304.8 - 15.9 - 6.4));
		assertThat(set.parts().stream().filter(part -> part.getRole() == PartRole.SHELF)
				.map(part -> part.getLengthMm() == round(shelfDepth) ? part.getWidthMm() : part.getLengthMm()))
				.as("two shelves per section, each as wide as its section")
				.containsExactly(round(300 - 15.9 * 1.5 - 2), round(300 - 15.9 * 1.5 - 2),
						round(914.4 - 300 - 15.9 * 1.5 - 2), round(914.4 - 300 - 15.9 * 1.5 - 2));
		assertThat(set.hardware()).containsEntry(SHELF_PIN.getId(), 16);
	}

	@Test
	void wideCabinetsGetTwoDoors() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("WALL_DOOR").templateVersion(1).wallId("w1")
				.widthMm(914.4).heightMm(762).depthMm(304.8).elevationMm(1371.6).shelfCount(2)
				.build();

		var set = PartGenerator.generate(cabinet, WALL_DOOR, "C1", STANDARD, MATERIALS);

		assertThat(count(set, PartRole.DOOR)).isEqualTo(2);
		assertThat(first(set, PartRole.DOOR).getWidthMm()).isEqualTo(round((914.4 - 6) / 2));
		assertThat(first(set, PartRole.DOOR).getLengthMm()).as("wall cabinets have no toe kick")
				.isEqualTo(round(762 - 3));
		assertThat(count(set, PartRole.TOP)).isEqualTo(1);
		assertThat(count(set, PartRole.STRETCHER)).isZero();
		assertThat(set.hardware())
				.containsEntry(HINGE.getId(), 4)
				.containsEntry(SHELF_PIN.getId(), 8)
				.doesNotContainKey(LEG.getId());
	}

	@Test
	void drawerBaseGetsFrontsSlidesAndBoxes() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("BASE_DRAWERS").templateVersion(1).wallId("w1")
				.widthMm(457.2).heightMm(876.3).depthMm(609.6)
				.build();

		var set = PartGenerator.generate(cabinet, BASE_DRAWERS, "C1", STANDARD, MATERIALS);

		assertThat(count(set, PartRole.DRAWER_FRONT)).isEqualTo(3);
		assertThat(count(set, PartRole.DOOR)).isZero();
		assertThat(count(set, PartRole.SHELF)).isZero();
		assertThat(set.hardware())
				.containsEntry(SLIDE.getId(), 3)
				.containsEntry(DRAWER_BOX.getId(), 3)
				.containsEntry(PULL.getId(), 3)
				.doesNotContainKey(HINGE.getId());
	}

	@Test
	void sinkHasAFalseFrontAboveItsDoorsAndNoPullOnIt() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("BASE_SINK").templateVersion(1).wallId("w1")
				.widthMm(914.4).heightMm(876.3).depthMm(609.6)
				.build();

		var set = PartGenerator.generate(cabinet, BASE_SINK, "C1", STANDARD, MATERIALS);

		var boxHeight = 876.3 - 101.6;
		assertThat(count(set, PartRole.FALSE_FRONT)).isEqualTo(1);
		assertThat(first(set, PartRole.FALSE_FRONT).getWidthMm()).isEqualTo(152.4);
		assertThat(count(set, PartRole.DOOR)).isEqualTo(2);
		assertThat(first(set, PartRole.DOOR).getLengthMm()).isEqualTo(round(boxHeight - 2 * 3 - 152.4));
		assertThat(set.hardware()).containsEntry(PULL.getId(), 2);
	}

	@Test
	void blindCornerFrontsCoverOnlyTheOpenSide() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("BASE_BLIND_CORNER").templateVersion(1).wallId("w1")
				.widthMm(1066.8).heightMm(876.3).depthMm(609.6).shelfCount(1)
				.build();

		var set = PartGenerator.generate(cabinet, BASE_BLIND_CORNER, "C1", STANDARD, MATERIALS);

		assertThat(count(set, PartRole.DOOR)).as("457.2 mm of front: one leaf").isEqualTo(1);
		assertThat(first(set, PartRole.DOOR).getWidthMm()).isEqualTo(round(1066.8 - 609.6 - 3));
		assertThat(first(set, PartRole.BLIND_PANEL).getWidthMm()).isEqualTo(round(609.6 - 3));
		assertThat(set.hardware()).containsEntry(LEG.getId(), 6);
	}

	@Test
	void ovenTowerLeavesTheOpeningEmpty() {
		var cabinet = Cabinet.builder()
				.id("c1").templateCode("TALL_OVEN").templateVersion(1).wallId("w1")
				.widthMm(762).heightMm(2286).depthMm(609.6).shelfCount(1)
				.build();

		var set = PartGenerator.generate(cabinet, TALL_OVEN, "C1", STANDARD, MATERIALS);

		assertThat(count(set, PartRole.DOOR)).as("the top door: 762 mm of front makes two leaves").isEqualTo(2);
		assertThat(first(set, PartRole.DOOR).getLengthMm()).isEqualTo(381);
		assertThat(count(set, PartRole.DRAWER_FRONT)).isEqualTo(2);
		assertThat(count(set, PartRole.TOP)).isEqualTo(1);
		assertThat(count(set, PartRole.STRETCHER)).isZero();
		assertThat(set.hardware()).containsEntry(SLIDE.getId(), 2).containsEntry(LEG.getId(), 4);
	}

	@Test
	void hingeCountGrowsWithDoorHeight() {
		assertThat(PartGenerator.hingesPerDoor(700)).isEqualTo(2);
		assertThat(PartGenerator.hingesPerDoor(1200)).isEqualTo(3);
		assertThat(PartGenerator.hingesPerDoor(2000)).isEqualTo(4);
	}

	private static long count(PartGenerator.PartSet set, PartRole role) {
		return set.parts().stream().filter(part -> part.getRole() == role).count();
	}

	private static Part first(PartGenerator.PartSet set, PartRole role) {
		return set.parts().stream().filter(part -> part.getRole() == role).findFirst().orElseThrow();
	}

	private static double round(double value) {
		return PartGenerator.round(value);
	}
}
