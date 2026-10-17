package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A strip of Front material closing a gap between Cabinets, or between a Cabinet and a Wall or
 * Appliance. Its face stands {@code depthMm} out from the Wall, flush with the Fronts beside it.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Filler {
	String id;
	String wallId;
	double offsetMm;
	double widthMm;
	double heightMm;
	double depthMm;
	double elevationMm;
}
