package dev.daniellavoie.opencabinet.project.domain.model;

/**
 * BASE cabinets stand on the floor on legs, WALL cabinets hang on the Wall, TALL cabinets stand on
 * the floor and rise to wall-cabinet height or above.
 */
public enum CabinetType {
	BASE, WALL, TALL;

	/**
	 * Whether the Cabinet stands on legs behind a toe kick.
	 */
	public boolean standsOnFloor() {
		return this != WALL;
	}
}
