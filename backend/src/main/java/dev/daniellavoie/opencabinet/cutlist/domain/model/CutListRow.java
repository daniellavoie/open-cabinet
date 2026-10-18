package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.PartRole;
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
	/**
	 * The template's name for an OTHER Part.
	 */
	String name;
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
