package dev.daniellavoie.opencabinet.cutlist.domain.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One physical piece of a Cabinet. {@code lengthMm} runs along the grain for grained materials and
 * is otherwise the longer side. Edge banding is counted per edge: up to two length edges and two
 * width edges.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Part {
	String cabinetId;
	String cabinetLabel;
	PartRole role;
	UUID materialId;
	double lengthMm;
	double widthMm;
	double thicknessMm;
	boolean grain;
	int bandedLengthEdges;
	int bandedWidthEdges;
	UUID edgeBandingId;
}
