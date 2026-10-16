package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A starting point for a type of Cabinet, with the dimension ranges the Construction Rules
 * enforce. Lengths are in millimetres.
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
	int drawerCount;
	boolean doors;
}
