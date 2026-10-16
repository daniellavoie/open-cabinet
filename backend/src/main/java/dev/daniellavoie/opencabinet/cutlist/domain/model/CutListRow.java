package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Identical Parts grouped together, with the Cabinets they belong to.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CutListRow {
	PartRole role;
	UUID materialId;
	String materialName;
	double lengthMm;
	double widthMm;
	double thicknessMm;
	boolean grain;
	int bandedLengthEdges;
	int bandedWidthEdges;
	UUID edgeBandingId;
	String edgeBandingName;
	int quantity;
	List<String> cabinetLabels;
}
