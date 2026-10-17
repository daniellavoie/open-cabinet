package dev.daniellavoie.opencabinet.drawing.domain.model;

/**
 * How a line is drawn: {@code WALL} heaviest, {@code OUTLINE} for what is built, {@code THIN} for
 * details, {@code HIDDEN} dashed for what stands above or behind, {@code NONE} for fill only.
 */
public enum Stroke {
	WALL, OUTLINE, THIN, HIDDEN, NONE
}
