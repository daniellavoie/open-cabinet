package dev.daniellavoie.opencabinet.cutlist.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.catalog.domain.model.MaterialKind;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.FrontRow;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

final class CutListFixtures {
	static final Material CARCASS = sheet("Carcass", "68.00", 15.9, 2463.8, 1244.6, false);
	static final Material BACK = sheet("Back", "38.00", 6.4, 2438.4, 1219.2, false);
	static final Material FRONT = sheet("Front", "95.00", 15.9, 2463.8, 1244.6, true);
	static final Material CARCASS_BANDING = other(MaterialKind.EDGE_BANDING, "Carcass banding", "0.45");
	static final Material FRONT_BANDING = other(MaterialKind.EDGE_BANDING, "Front banding", "0.85");
	static final Material HINGE = other(MaterialKind.HARDWARE, "Hinge", "6.50");
	static final Material SHELF_PIN = other(MaterialKind.HARDWARE, "Shelf pin", "0.15");
	static final Material LEG = other(MaterialKind.HARDWARE, "Leg", "2.20");
	static final Material PULL = other(MaterialKind.HARDWARE, "Pull", "8.00");
	static final Material SLIDE = other(MaterialKind.HARDWARE, "Slide", "48.00");
	static final Material DRAWER_BOX = other(MaterialKind.HARDWARE, "Drawer box", "45.00");
	static final Material SHAKER = other(MaterialKind.FRONT, "Shaker", "300.00");
	static final Material SLAB = other(MaterialKind.FRONT, "Slab", "200.00");
	static final Material CROWN = other(MaterialKind.MOULDING, "Crown", "9.50");

	static final List<Material> CATALOG = List.of(CARCASS, BACK, FRONT, CARCASS_BANDING, FRONT_BANDING, HINGE,
			SHELF_PIN, LEG, PULL, SLIDE, DRAWER_BOX, SHAKER, SLAB, CROWN);

	static final ConstructionStandard STANDARD = ConstructionStandard.builder()
			.carcassMaterialId(CARCASS.getId())
			.backMaterialId(BACK.getId())
			.frontMaterialId(FRONT.getId())
			.orderedFrontId(SHAKER.getId())
			.crownMouldingId(CROWN.getId())
			.carcassEdgeBandingId(CARCASS_BANDING.getId())
			.frontEdgeBandingId(FRONT_BANDING.getId())
			.hingeId(HINGE.getId())
			.shelfPinId(SHELF_PIN.getId())
			.legId(LEG.getId())
			.pullId(PULL.getId())
			.drawerSlideId(SLIDE.getId())
			.drawerBoxId(DRAWER_BOX.getId())
			.doorGapMm(3)
			.toeKickHeightMm(101.6)
			.kerfMm(4)
			.sheetTrimMm(10)
			.build();

	static final CabinetTemplate BASE_DOOR = CabinetTemplate.builder()
			.code("BASE_DOOR").type(CabinetType.BASE)
			.minWidthMm(228.6).maxWidthMm(914.4).minHeightMm(762).maxHeightMm(965.2)
			.minDepthMm(304.8).maxDepthMm(762).maxShelfCount(4).frontRows(List.of(row(FrontKind.DOOR, null)))
			.build();

	static final CabinetTemplate BASE_DRAWERS = BASE_DOOR.toBuilder()
			.code("BASE_DRAWERS").maxShelfCount(0)
			.frontRows(List.of(row(FrontKind.DRAWER, 152.4), row(FrontKind.DRAWER, null), row(FrontKind.DRAWER, null)))
			.build();

	static final CabinetTemplate BASE_SINK = BASE_DOOR.toBuilder()
			.code("BASE_SINK").maxShelfCount(0)
			.frontRows(List.of(row(FrontKind.FALSE_FRONT, 152.4), row(FrontKind.DOOR, null)))
			.build();

	static final CabinetTemplate BASE_BLIND_CORNER = BASE_DOOR.toBuilder()
			.code("BASE_BLIND_CORNER").maxWidthMm(1219.2).blindWidthMm(609.6)
			.build();

	static final CabinetTemplate TALL_OVEN = CabinetTemplate.builder()
			.code("TALL_OVEN").type(CabinetType.TALL)
			.minWidthMm(762).maxWidthMm(838.2).minHeightMm(2133.6).maxHeightMm(2438.4)
			.minDepthMm(609.6).maxDepthMm(762).maxShelfCount(2)
			.frontRows(List.of(row(FrontKind.DOOR, 381.0), row(FrontKind.OPENING, 762.0), row(FrontKind.DRAWER, null),
					row(FrontKind.DRAWER, null)))
			.build();

	static final CabinetTemplate WALL_DOOR = CabinetTemplate.builder()
			.code("WALL_DOOR").type(CabinetType.WALL)
			.minWidthMm(228.6).maxWidthMm(1219.2).minHeightMm(304.8).maxHeightMm(1066.8)
			.minDepthMm(228.6).maxDepthMm(609.6).maxShelfCount(4).frontRows(List.of(row(FrontKind.DOOR, null)))
			.build();

	private CutListFixtures() {
	}

	static FrontRow row(FrontKind kind, Double heightMm) {
		return FrontRow.builder().kind(kind).heightMm(heightMm).build();
	}

	private static Material sheet(String name, String price, double thickness, double length, double width,
			boolean grain) {
		return Material.builder()
				.id(UUID.randomUUID())
				.kind(MaterialKind.SHEET)
				.name(name)
				.price(new BigDecimal(price))
				.thicknessMm(thickness)
				.lengthMm(length)
				.widthMm(width)
				.grain(grain)
				.build();
	}

	private static Material other(MaterialKind kind, String name, String price) {
		return Material.builder()
				.id(UUID.randomUUID())
				.kind(kind)
				.name(name)
				.price(new BigDecimal(price))
				.build();
	}
}
