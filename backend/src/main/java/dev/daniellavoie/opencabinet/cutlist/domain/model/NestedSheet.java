package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class NestedSheet {
	List<SheetPlacement> placements;
	/**
	 * Share of the sheet's area covered by Parts, from 0 to 1.
	 */
	double utilization;
}
