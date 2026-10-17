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
	String wallId;
	double offsetMm;
	double widthMm;
	double heightMm;
	double depthMm;
	double elevationMm;
	int shelfCount;
	/**
	 * Which side of a blind corner Cabinet is left without fronts.
	 */
	Side blindSide;
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

	public List<Double> dividers() {
		return dividerPositionsMm == null ? List.of() : dividerPositionsMm;
	}

	public boolean frontsOrdered() {
		return frontSource == FrontSource.ORDERED;
	}

	public Side effectiveBlindSide() {
		return blindSide == null ? Side.RIGHT : blindSide;
	}
}
