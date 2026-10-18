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
	 * Openings cut through a Wall in plan view, and windows on Walls in 3D: filled white.
	 */
	OPENING,
	/**
	 * Faces in 3D, filled lightest to darkest so a box reads as a solid: tops, faces turned to the
	 * reader, faces turned away.
	 */
	FACE_LIGHT,
	FACE_MEDIUM,
	FACE_DARK
}
