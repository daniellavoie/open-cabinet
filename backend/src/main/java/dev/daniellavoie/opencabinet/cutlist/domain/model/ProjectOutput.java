package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Everything computed from a Project's design: its Cut List, Nesting and Bill of Materials.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ProjectOutput {
	/**
	 * Cabinet id to its label (C1, C2, …), in reading order.
	 */
	Map<String, String> cabinetLabels;
	List<CutListRow> cutList;
	List<MaterialNesting> nesting;
	List<BomLine> billOfMaterials;
	BigDecimal materialsTotal;
	OperationCounts operations;
}
