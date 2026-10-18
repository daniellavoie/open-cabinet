package dev.daniellavoie.opencabinet.drawing.domain.model;

public enum Stroke {
	/**
	 * Edges of what the Shop builds: Cabinets and Fillers.
	 */
	OUTLINE,
	/**
	 * Fronts, Openings and the Room's outline.
	 */
	THIN,
	/**
	 * What is hidden or not built by the Shop: wall Cabinets in plan view, Appliances.
	 */
	HIDDEN,
	/**
	 * Walls in plan view, filled.
	 */
	WALL,
	/**
	 * Openings cut through a Wall in plan view.
	 */
	OPENING
}
