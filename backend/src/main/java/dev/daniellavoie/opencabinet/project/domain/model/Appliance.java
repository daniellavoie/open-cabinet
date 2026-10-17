package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A placeholder for an appliance against a Wall. Cabinets are fitted around it; the Shop does not
 * build it.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Appliance {
	String id;
	ApplianceKind kind;
	String wallId;
	double offsetMm;
	double widthMm;
	double heightMm;
	double depthMm;
	double elevationMm;
}
