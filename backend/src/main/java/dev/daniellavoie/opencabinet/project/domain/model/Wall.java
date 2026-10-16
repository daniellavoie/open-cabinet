package dev.daniellavoie.opencabinet.project.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A straight Wall segment. Walls are laid out in order, each starting where the previous one ends;
 * {@code angleToNextDegrees} is the counter-clockwise turn, seen from above, to the next Wall.
 * Cabinets stand on the left side of the Wall's direction.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Wall {
	String id;
	double lengthMm;
	double heightMm;
	double angleToNextDegrees;
}
