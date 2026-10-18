package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Cabinet placed against a Wall: {@code offsetMm} is the distance from the Wall's start to the
 * Cabinet's left side, {@code elevationMm} the height of its bottom above the floor.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Cabinet {
	String id;
	String templateCode;
	/**
	 * The Template Version the Cabinet keeps until a Member updates or reverts it.
	 */
	int templateVersion;
	String wallId;
	double offsetMm;
	double widthMm;
	double heightMm;
	double depthMm;
	double elevationMm;
	int shelfCount;
	/**
	 * Built left for right: a blind corner then has its blind side on the left.
	 */
	boolean mirrored;
	/**
	 * Vertical dividers splitting the interior into sections, as the distance from the Cabinet's
	 * left side to each divider's centre, left to right. Each section gets {@code shelfCount}
	 * shelves.
	 */
	List<Double> dividerPositionsMm;
	/**
	 * Made in the Shop when unset.
	 */
	FrontSource frontSource;
	/**
	 * The ordered Front style (a FRONT catalog entry), when not the Construction Standard's.
	 */
	UUID orderedFrontId;
	/**
	 * Finished end panels applied over an exposed side.
	 */
	boolean finishedLeft;
	boolean finishedRight;
	/**
	 * Crown moulding along the top front, for wall and tall Cabinets.
	 */
	boolean crown;

	public String templateKey() {
		return CabinetTemplate.key(templateCode, templateVersion);
	}

	public List<Double> dividers() {
		return dividerPositionsMm == null ? List.of() : dividerPositionsMm;
	}

	public boolean frontsOrdered() {
		return frontSource == FrontSource.ORDERED;
	}

	/**
	 * The side of a blind corner Cabinet left without fronts.
	 */
	public Side effectiveBlindSide() {
		return mirrored ? Side.LEFT : Side.RIGHT;
	}
}
