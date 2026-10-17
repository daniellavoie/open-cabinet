package dev.daniellavoie.opencabinet.catalog.domain.model;

/**
 * How a Material is bought and priced: SHEET per sheet, EDGE_BANDING per metre, HARDWARE per unit,
 * FRONT (a style of Front ordered from a supplier, made to size) per square metre.
 */
public enum MaterialKind {
	SHEET, EDGE_BANDING, HARDWARE, FRONT
}
