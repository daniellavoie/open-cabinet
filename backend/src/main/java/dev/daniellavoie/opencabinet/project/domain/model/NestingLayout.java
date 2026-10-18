package dev.daniellavoie.opencabinet.project.domain.model;

/**
 * How sheets are first cut. Both are guillotine layouts a panel saw can cut.
 */
public enum NestingLayout {
	/**
	 * Rip the sheet along its length into strips, then crosscut each strip.
	 */
	RIP_FIRST,
	/**
	 * Crosscut the sheet into sections, then rip each section.
	 */
	CROSSCUT_FIRST
}
