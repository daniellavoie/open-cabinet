package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A starting point for a type of Cabinet, with the dimension ranges the Construction Rules
 * enforce. Lengths are in millimetres. {@code frontRows} lay out the front from the top;
 * {@code blindWidthMm} is the width at one side left without fronts, for blind corners.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CabinetTemplate {
	String code;
	CabinetType type;
	double defaultWidthMm;
	double minWidthMm;
	double maxWidthMm;
	double defaultHeightMm;
	double minHeightMm;
	double maxHeightMm;
	double defaultDepthMm;
	double minDepthMm;
	double maxDepthMm;
	double defaultElevationMm;
	int defaultShelfCount;
	int maxShelfCount;
	List<FrontRow> frontRows;
	double blindWidthMm;

	/**
	 * Dividers fit behind doors only: drawers, false fronts and appliance openings span the width.
	 */
	public boolean allowsDividers() {
		return frontRows != null && !frontRows.isEmpty()
				&& frontRows.stream().allMatch(row -> row.getKind() == FrontKind.DOOR);
	}
}
