package dev.daniellavoie.opencabinet.shop.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Shop's way of building Frameless cabinets: which catalog materials and hardware go where, and
 * the dimensions the Construction Rules and part generation use. Lengths are in millimetres.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ConstructionStandard {
	UUID carcassMaterialId;
	UUID backMaterialId;
	UUID frontMaterialId;
	UUID carcassEdgeBandingId;
	UUID frontEdgeBandingId;
	/**
	 * The Front style ordered for Cabinets whose Fronts are ordered, unless they pick another.
	 */
	UUID orderedFrontId;
	/**
	 * The crown moulding put on Cabinets that ask for it.
	 */
	UUID crownMouldingId;

	UUID hingeId;
	UUID shelfPinId;
	UUID legId;
	UUID pullId;
	UUID drawerSlideId;
	UUID drawerBoxId;

	double doorGapMm;
	double toeKickHeightMm;
	double kerfMm;
	double sheetTrimMm;
}
