package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;
import java.util.UUID;

import dev.daniellavoie.opencabinet.project.domain.model.NestingLayout;
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
	/**
	 * The layout these sheets use.
	 */
	NestingLayout layout;
	List<NestedSheet> sheets;
	/**
	 * Parts larger than the sheet, which cannot be cut from it.
	 */
	List<Part> oversizedParts;
	/**
	 * Every layout's result for this material, the one used included.
	 */
	List<NestingAlternative> alternatives;
}
