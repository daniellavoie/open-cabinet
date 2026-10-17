package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One Template Version of a kind of Cabinet, with the dimension ranges the Construction Rules
 * enforce. Lengths are in millimetres. {@code frontRows} lay out the front from the top;
 * {@code blindWidthMm} is the width at one side left without fronts, for blind corners. A
 * published version never changes (docs/adr/0005-cabinet-keeps-its-template-version.md):
 * {@code changeNote} says what changed from the previous version.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class CabinetTemplate {
	String code;
	int version;
	String changeNote;
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
	 * Identifies a Template Version: its template's code and its number.
	 */
	public static String key(String code, int version) {
		return code + "@" + version;
	}

	public String key() {
		return key(code, version);
	}

	/**
	 * Dividers fit behind doors only: drawers, false fronts and appliance openings span the width.
	 */
	public boolean allowsDividers() {
		return frontRows != null && !frontRows.isEmpty()
				&& frontRows.stream().allMatch(row -> row.getKind() == FrontKind.DOOR);
	}
}
