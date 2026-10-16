package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The Nesting of every Part cut from one sheet material.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class MaterialNesting {
	UUID materialId;
	String materialName;
	double sheetLengthMm;
	double sheetWidthMm;
	boolean grain;
	List<NestedSheet> sheets;
	/**
	 * Parts larger than the sheet, which cannot be cut from it.
	 */
	List<Part> oversizedParts;
}
