package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A window or doorway in a Wall that Cabinets must not cover, positioned like a Cabinet: from the
 * Wall's start and above the floor.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Opening {
	String id;
	OpeningKind kind;
	String wallId;
	double offsetMm;
	double widthMm;
	double elevationMm;
	double heightMm;
}
