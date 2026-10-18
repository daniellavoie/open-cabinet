package dev.daniellavoie.opencabinet.project.domain;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.MaterialRef;
import dev.daniellavoie.opencabinet.project.domain.model.TemplateAssembly.StandardValue;
import dev.daniellavoie.opencabinet.shop.domain.model.ConstructionStandard;

/**
 * Connects an assembled template to a Shop: its material roles are the Construction Standard's
 * materials, its Standard values the Standard's lengths, and a Cabinet gives the size and choices.
 */
public final class CabinetAssemblies {
	private CabinetAssemblies() {
	}

	/**
	 * The catalog material a template's material stands for in this Shop.
	 */
	public static UUID materialId(MaterialRef material, ConstructionStandard standard) {
		if (material.materialId() != null) {
			return material.materialId();
		}
		return switch (material.role()) {
		case CARCASS -> standard.getCarcassMaterialId();
		case BACK -> standard.getBackMaterialId();
		case FRONT -> standard.getFrontMaterialId();
		case CARCASS_BANDING -> standard.getCarcassEdgeBandingId();
		case FRONT_BANDING -> standard.getFrontEdgeBandingId();
		case CROWN_MOULDING -> standard.getCrownMouldingId();
		case HINGE -> standard.getHingeId();
		case SHELF_PIN -> standard.getShelfPinId();
		case LEG -> standard.getLegId();
		case PULL -> standard.getPullId();
		case DRAWER_SLIDE -> standard.getDrawerSlideId();
		case DRAWER_BOX -> standard.getDrawerBoxId();
		};
	}

	public static Map<StandardValue, Double> standardValues(ConstructionStandard standard) {
		return Map.of(StandardValue.DOOR_GAP, standard.getDoorGapMm(), StandardValue.TOE_KICK_HEIGHT,
				standard.getToeKickHeightMm());
	}

	/**
	 * What a placed Cabinet brings to its template's assembly; {@code thickness} gives a catalog
	 * material's thickness.
	 */
	public static Assembly.Context context(Cabinet cabinet, ConstructionStandard standard,
			Function<UUID, Double> thickness) {
		return new Assembly.Context(cabinet.getWidthMm(), cabinet.getHeightMm(), cabinet.getDepthMm(),
				cabinet.getShelfCount(), cabinet.dividers(), standardValues(standard),
				material -> thickness.apply(materialId(material, standard)));
	}
}
