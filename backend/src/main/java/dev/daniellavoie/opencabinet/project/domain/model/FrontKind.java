package dev.daniellavoie.opencabinet.project.domain.model;

/**
 * What fills one row of a Cabinet's front: a door (one or two leaves), a drawer, a fixed false
 * front (e.g. above a sink) or an opening left empty for an appliance.
 */
public enum FrontKind {
	DOOR, DRAWER, FALSE_FRONT, OPENING
}
