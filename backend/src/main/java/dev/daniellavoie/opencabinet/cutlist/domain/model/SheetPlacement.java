package dev.daniellavoie.opencabinet.cutlist.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Where one Part sits on a sheet. {@code x} runs along the sheet's length (and its grain), {@code y}
 * across its width, both from the sheet's corner.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class SheetPlacement {
	String cabinetLabel;
	PartRole role;
	double xMm;
	double yMm;
	double lengthMm;
	double widthMm;
	boolean rotated;
}
