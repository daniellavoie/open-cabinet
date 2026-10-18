package dev.daniellavoie.opencabinet.cutlist.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One saw cut across a sheet or a piece of it, numbered in cutting order: 1, 2… for the first cuts,
 * 1.1, 1.2… for the cuts of piece 1, 1.1.1… for the cuts of piece 1.1. Coordinates as for
 * placements, from the sheet's corner.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class SheetCut {
	String number;
	double fromAlongMm;
	double fromAcrossMm;
	double toAlongMm;
	double toAcrossMm;
}
