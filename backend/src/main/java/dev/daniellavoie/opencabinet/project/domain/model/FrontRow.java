package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * One row of a Cabinet's front, listed from the top. A row has either a fixed {@code heightMm} or
 * shares the remaining height with the other flexible rows in proportion to its {@code weight}
 * (1 when unset).
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class FrontRow {
	FrontKind kind;
	Double heightMm;
	double weight;

	public double effectiveWeight() {
		return weight > 0 ? weight : 1;
	}
}
