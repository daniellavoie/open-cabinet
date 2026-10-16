package dev.daniellavoie.opencabinet.cutlist.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import dev.daniellavoie.opencabinet.catalog.domain.model.Material;
import dev.daniellavoie.opencabinet.cutlist.domain.model.Part;
import dev.daniellavoie.opencabinet.cutlist.domain.model.PartRole;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

/**
 * Breaks a Frameless Cabinet into Parts and hardware, following the Shop's Construction Standard.
 *
 * <p>
 * The box is built from two full-height sides with the bottom and top (or two stretchers on base
 * cabinets) between them, and the back applied over the whole rear. Base cabinets stand on legs, so
 * their box is the cabinet height minus the toe kick. Fronts are full overlay.
 */
final class PartGenerator {
	static final double STRETCHER_WIDTH_MM = 101.6;
	static final double SHELF_SIDE_CLEARANCE_MM = 2;
	static final double SHELF_SETBACK_MM = 25;
	static final double DOUBLE_DOOR_MIN_WIDTH_MM = 609.6;
	static final double WIDE_BASE_MIN_WIDTH_MM = 914.4;

	record PartSet(List<Part> parts, Map<UUID, Integer> hardware) {
	}

	private PartGenerator() {
	}

	static PartSet generate(Cabinet cabinet, CabinetTemplate template, String label, ConstructionStandard standard,
			Map<UUID, Material> catalog) {
		var carcass = material(catalog, standard.getCarcassMaterialId());
		var back = material(catalog, standard.getBackMaterialId());
		var front = material(catalog, standard.getFrontMaterialId());

		var t = carcass.getThicknessMm();
		var gap = standard.getDoorGapMm();
		var width = cabinet.getWidthMm();
		var boxHeight = template.getType() == CabinetType.BASE
				? cabinet.getHeightMm() - standard.getToeKickHeightMm()
				: cabinet.getHeightMm();
		var carcassDepth = cabinet.getDepthMm() - front.getThicknessMm() - back.getThicknessMm();
		var innerWidth = width - 2 * t;

		var parts = new ArrayList<Part>();
		var hardware = new LinkedHashMap<UUID, Integer>();
		var builder = new Builder(cabinet, label, parts);

		builder.add(PartRole.SIDE, carcass, boxHeight, carcassDepth, 1, 0, standard.getCarcassEdgeBandingId(), 2);
		builder.add(PartRole.BOTTOM, carcass, innerWidth, carcassDepth, 1, 0, standard.getCarcassEdgeBandingId(), 1);
		if (template.getType() == CabinetType.BASE) {
			builder.add(PartRole.STRETCHER, carcass, innerWidth, STRETCHER_WIDTH_MM, 1, 0,
					standard.getCarcassEdgeBandingId(), 2);
		} else {
			builder.add(PartRole.TOP, carcass, innerWidth, carcassDepth, 1, 0, standard.getCarcassEdgeBandingId(), 1);
		}
		builder.add(PartRole.BACK, back, width, boxHeight, 0, 0, null, 1);

		if (cabinet.getShelfCount() > 0) {
			builder.add(PartRole.SHELF, carcass, innerWidth - SHELF_SIDE_CLEARANCE_MM,
					carcassDepth - SHELF_SETBACK_MM, 1, 0, standard.getCarcassEdgeBandingId(), cabinet.getShelfCount());
			add(hardware, standard.getShelfPinId(), 4 * cabinet.getShelfCount());
		}

		var fronts = 0;
		if (template.isDoors()) {
			var doors = width > DOUBLE_DOOR_MIN_WIDTH_MM ? 2 : 1;
			var doorHeight = boxHeight - gap;
			builder.add(PartRole.DOOR, front, doorHeight, (width - gap * doors) / doors, 2, 2,
					standard.getFrontEdgeBandingId(), doors);
			add(hardware, standard.getHingeId(), doors * hingesPerDoor(doorHeight));
			fronts += doors;
		}

		var drawers = template.getDrawerCount();
		if (drawers > 0) {
			var frontHeight = (boxHeight - gap * drawers) / drawers;
			builder.add(PartRole.DRAWER_FRONT, front, width - gap, frontHeight, 2, 2,
					standard.getFrontEdgeBandingId(), drawers);
			add(hardware, standard.getDrawerSlideId(), drawers);
			add(hardware, standard.getDrawerBoxId(), drawers);
			fronts += drawers;
		}

		add(hardware, standard.getPullId(), fronts);

		if (template.getType() == CabinetType.BASE) {
			add(hardware, standard.getLegId(), width > WIDE_BASE_MIN_WIDTH_MM ? 6 : 4);
		}

		return new PartSet(parts, hardware);
	}

	static int hingesPerDoor(double doorHeightMm) {
		if (doorHeightMm <= 1000) {
			return 2;
		}
		return doorHeightMm <= 1600 ? 3 : 4;
	}

	private static void add(Map<UUID, Integer> hardware, UUID materialId, int quantity) {
		if (quantity > 0) {
			hardware.merge(materialId, quantity, Integer::sum);
		}
	}

	private static Material material(Map<UUID, Material> catalog, UUID materialId) {
		return Optional.ofNullable(materialId)
				.map(catalog::get)
				.orElseThrow(() -> new NoSuchElementException(
						"material " + materialId + " of the construction standard is not in the catalog"));
	}

	private record Builder(Cabinet cabinet, String label, List<Part> parts) {
		/**
		 * Adds {@code quantity} Parts. {@code grainSide} is the dimension the grain should follow;
		 * banded edge counts refer to the edges along each of the two given dimensions.
		 */
		void add(PartRole role, Material material, double grainSide, double otherSide, int bandedGrainEdges,
				int bandedOtherEdges, UUID edgeBandingId, int quantity) {
			var keepOrientation = material.isGrain() || grainSide >= otherSide;
			var hasBanding = edgeBandingId != null && bandedGrainEdges + bandedOtherEdges > 0;

			for (int index = 0; index < quantity; index++) {
				parts.add(Part.builder()
						.cabinetId(cabinet.getId())
						.cabinetLabel(label)
						.role(role)
						.materialId(material.getId())
						.lengthMm(round(keepOrientation ? grainSide : otherSide))
						.widthMm(round(keepOrientation ? otherSide : grainSide))
						.thicknessMm(material.getThicknessMm())
						.grain(material.isGrain())
						.bandedLengthEdges(hasBanding ? (keepOrientation ? bandedGrainEdges : bandedOtherEdges) : 0)
						.bandedWidthEdges(hasBanding ? (keepOrientation ? bandedOtherEdges : bandedGrainEdges) : 0)
						.edgeBandingId(hasBanding ? edgeBandingId : null)
						.build());
			}
		}
	}

	static double round(double millimetres) {
		return Math.round(millimetres * 10) / 10d;
	}
}
