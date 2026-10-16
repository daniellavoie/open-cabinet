package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet placed against a Wall: {@code offsetMm} is the distance from the Wall's start to the
 * Cabinet's left side, {@code elevationMm} the height of its bottom above the floor.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Cabinet {
	String id;
	String templateCode;
	String wallId;
	double offsetMm;
	double widthMm;
	double heightMm;
	double depthMm;
	double elevationMm;
	int shelfCount;
}
