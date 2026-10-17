package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.project.domain.model.Appliance;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
import dev.daniellavoie.opencabinet.project.domain.model.Opening;
import dev.daniellavoie.opencabinet.project.domain.model.Side;
import dev.daniellavoie.opencabinet.project.domain.model.Violation;
import dev.daniellavoie.opencabinet.project.domain.model.Wall;

/**
 * The Construction Rules a Design must satisfy before it is saved.
 *
 * <p>
 * Implemented twice (docs/adr/0002-construction-rules-run-in-browser-and-server.md): this class is
 * the authority, and {@code construction-rules.ts} in the Angular app gives live feedback. Both
 * must return the same violations, in the same order, for every case in
 * {@code src/test/resources/construction-rules/cases.json}.
 */
public final class ConstructionRules {
	public static final String INVALID_WALL = "INVALID_WALL";
	public static final String UNKNOWN_WALL = "UNKNOWN_WALL";
	public static final String UNKNOWN_TEMPLATE = "UNKNOWN_TEMPLATE";
	public static final String WIDTH_OUT_OF_RANGE = "WIDTH_OUT_OF_RANGE";
	public static final String HEIGHT_OUT_OF_RANGE = "HEIGHT_OUT_OF_RANGE";
	public static final String DEPTH_OUT_OF_RANGE = "DEPTH_OUT_OF_RANGE";
	public static final String SHELF_COUNT_OUT_OF_RANGE = "SHELF_COUNT_OUT_OF_RANGE";
	public static final String BASE_NOT_ON_FLOOR = "BASE_NOT_ON_FLOOR";
	public static final String BELOW_FLOOR = "BELOW_FLOOR";
	public static final String OUTSIDE_WALL = "OUTSIDE_WALL";
	public static final String ABOVE_WALL = "ABOVE_WALL";
	public static final String OVERLAP = "OVERLAP";
	public static final String ITEM_OUTSIDE_WALL = "ITEM_OUTSIDE_WALL";
	public static final String BLOCKS_OPENING = "BLOCKS_OPENING";
	public static final String OVERLAPS_APPLIANCE = "OVERLAPS_APPLIANCE";
	public static final String CORNER_OVERLAP = "CORNER_OVERLAP";
	public static final String FRONT_BLOCKED = "FRONT_BLOCKED";
	public static final String DIVIDERS_NOT_ALLOWED = "DIVIDERS_NOT_ALLOWED";
	public static final String SECTION_TOO_NARROW = "SECTION_TOO_NARROW";

	/**
	 * Slack allowed on every comparison, in millimetres, so values rounded for display still pass.
	 */
	static final double TOLERANCE_MM = 0.5;

	/**
	 * Depth of the strip in front of a Cabinet's fronts that nothing may stand in.
	 */
	static final double FRONT_STRIP_MM = 2;

	/**
	 * Narrowest section between dividers (or a divider and a side), centre to centre.
	 */
	static final double MIN_SECTION_MM = 152.4;

	private ConstructionRules() {
	}

	public static List<Violation> check(Design design, List<CabinetTemplate> templates) {
		var violations = new ArrayList<Violation>();
		var walls = Optional.ofNullable(design.getWalls()).orElse(List.of());
		var cabinets = Optional.ofNullable(design.getCabinets()).orElse(List.of());
		var openings = Optional.ofNullable(design.getOpenings()).orElse(List.of());
		var appliances = Optional.ofNullable(design.getAppliances()).orElse(List.of());
		Map<String, Wall> wallsById = walls.stream()
				.collect(Collectors.toMap(Wall::getId, Function.identity(), (first, second) -> first));
		Map<String, CabinetTemplate> templatesByCode = templates.stream()
				.collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity()));

		for (var wall : walls) {
			if (wall.getLengthMm() <= 0 || wall.getHeightMm() <= 0) {
				violations.add(wallViolation(INVALID_WALL, wall));
			}
		}

		for (var opening : openings) {
			var wall = wallsById.get(opening.getWallId());
			if (wall == null) {
				violations.add(itemViolation(UNKNOWN_WALL, opening.getWallId(), opening.getId()));
			} else if (outsideWall(opening.getOffsetMm(), opening.getWidthMm(), opening.getElevationMm(),
					opening.getHeightMm(), wall)) {
				violations.add(itemViolation(ITEM_OUTSIDE_WALL, opening.getWallId(), opening.getId()));
			}
		}

		for (var appliance : appliances) {
			var wall = wallsById.get(appliance.getWallId());
			if (wall == null) {
				violations.add(itemViolation(UNKNOWN_WALL, appliance.getWallId(), appliance.getId()));
			} else if (outsideWall(appliance.getOffsetMm(), appliance.getWidthMm(), appliance.getElevationMm(),
					appliance.getHeightMm(), wall)) {
				violations.add(itemViolation(ITEM_OUTSIDE_WALL, appliance.getWallId(), appliance.getId()));
			}
		}

		for (var cabinet : cabinets) {
			var wall = wallsById.get(cabinet.getWallId());
			if (wall == null) {
				violations.add(cabinetViolation(UNKNOWN_WALL, cabinet));
				continue;
			}

			var template = templatesByCode.get(cabinet.getTemplateCode());
			if (template == null) {
				violations.add(cabinetViolation(UNKNOWN_TEMPLATE, cabinet));
				continue;
			}

			if (outside(cabinet.getWidthMm(), template.getMinWidthMm(), template.getMaxWidthMm())) {
				violations.add(cabinetViolation(WIDTH_OUT_OF_RANGE, cabinet));
			}
			if (outside(cabinet.getHeightMm(), template.getMinHeightMm(), template.getMaxHeightMm())) {
				violations.add(cabinetViolation(HEIGHT_OUT_OF_RANGE, cabinet));
			}
			if (outside(cabinet.getDepthMm(), template.getMinDepthMm(), template.getMaxDepthMm())) {
				violations.add(cabinetViolation(DEPTH_OUT_OF_RANGE, cabinet));
			}
			if (cabinet.getShelfCount() < 0 || cabinet.getShelfCount() > template.getMaxShelfCount()) {
				violations.add(cabinetViolation(SHELF_COUNT_OUT_OF_RANGE, cabinet));
			}
			if (!cabinet.dividers().isEmpty() && !template.allowsDividers()) {
				violations.add(cabinetViolation(DIVIDERS_NOT_ALLOWED, cabinet));
			} else if (sectionTooNarrow(cabinet)) {
				violations.add(cabinetViolation(SECTION_TOO_NARROW, cabinet));
			}
			if (template.getType().standsOnFloor() && Math.abs(cabinet.getElevationMm()) > TOLERANCE_MM) {
				violations.add(cabinetViolation(BASE_NOT_ON_FLOOR, cabinet));
			} else if (cabinet.getElevationMm() < -TOLERANCE_MM) {
				violations.add(cabinetViolation(BELOW_FLOOR, cabinet));
			}
			if (cabinet.getOffsetMm() < -TOLERANCE_MM
					|| cabinet.getOffsetMm() + cabinet.getWidthMm() > wall.getLengthMm() + TOLERANCE_MM) {
				violations.add(cabinetViolation(OUTSIDE_WALL, cabinet));
			}
			if (cabinet.getElevationMm() + cabinet.getHeightMm() > wall.getHeightMm() + TOLERANCE_MM) {
				violations.add(cabinetViolation(ABOVE_WALL, cabinet));
			}
			if (openings.stream().anyMatch(opening -> opening.getWallId().equals(cabinet.getWallId())
					&& covers(cabinet, opening.getOffsetMm(), opening.getWidthMm(), opening.getElevationMm(),
							opening.getHeightMm()))) {
				violations.add(cabinetViolation(BLOCKS_OPENING, cabinet));
			}
		}

		for (int second = 0; second < cabinets.size(); second++) {
			for (int first = 0; first < second; first++) {
				if (overlap(cabinets.get(first), cabinets.get(second), wallsById)) {
					violations.add(cabinetViolation(OVERLAP, cabinets.get(second)));
					break;
				}
			}
		}

		for (var cabinet : cabinets) {
			if (wallsById.containsKey(cabinet.getWallId()) && appliances.stream()
					.anyMatch(appliance -> appliance.getWallId().equals(cabinet.getWallId())
							&& covers(cabinet, appliance.getOffsetMm(), appliance.getWidthMm(),
									appliance.getElevationMm(), appliance.getHeightMm()))) {
				violations.add(cabinetViolation(OVERLAPS_APPLIANCE, cabinet));
			}
		}

		for (int second = 0; second < appliances.size(); second++) {
			var later = appliances.get(second);
			for (int first = 0; first < second; first++) {
				var earlier = appliances.get(first);
				if (wallsById.containsKey(later.getWallId()) && earlier.getWallId().equals(later.getWallId())
						&& intersect(earlier.getOffsetMm(), earlier.getWidthMm(), later.getOffsetMm(), later.getWidthMm())
						&& intersect(earlier.getElevationMm(), earlier.getHeightMm(), later.getElevationMm(),
								later.getHeightMm())) {
					violations.add(itemViolation(OVERLAP, later.getWallId(), later.getId()));
					break;
				}
			}
		}

		checkCorners(design, cabinets, appliances, templatesByCode, violations);

		return violations;
	}

	/**
	 * Rules across Walls, in plan view. A Cabinet must not overlap a Cabinet or Appliance on another
	 * Wall at the same height, nor have one standing right in front of its fronts; the blind part of
	 * a blind corner Cabinet is meant to be covered by the neighbouring run. On a single Wall, the
	 * overlap rules above already cover both.
	 */
	private static void checkCorners(Design design, List<Cabinet> cabinets, List<Appliance> appliances,
			Map<String, CabinetTemplate> templatesByCode, List<Violation> violations) {
		var placements = RoomLayout.layout(Optional.ofNullable(design.getWalls()).orElse(List.of()));
		var footprints = new ArrayList<Footprint>();
		for (var cabinet : cabinets) {
			var placement = placements.get(cabinet.getWallId());
			if (placement != null) {
				footprints.add(new Footprint(cabinet.getId(), cabinet.getWallId(),
						placement.rectangle(cabinet.getOffsetMm(), cabinet.getOffsetMm() + cabinet.getWidthMm(), 0,
								cabinet.getDepthMm()),
						cabinet.getElevationMm(), cabinet.getHeightMm()));
			}
		}
		for (var appliance : appliances) {
			var placement = placements.get(appliance.getWallId());
			if (placement != null) {
				footprints.add(new Footprint(appliance.getId(), appliance.getWallId(),
						placement.rectangle(appliance.getOffsetMm(), appliance.getOffsetMm() + appliance.getWidthMm(),
								0, appliance.getDepthMm()),
						appliance.getElevationMm(), appliance.getHeightMm()));
			}
		}

		for (var cabinet : cabinets) {
			var placement = placements.get(cabinet.getWallId());
			if (placement == null) {
				continue;
			}
			var own = footprints.stream().filter(footprint -> footprint.id().equals(cabinet.getId())).findFirst()
					.orElseThrow();
			if (footprints.stream().anyMatch(other -> other != own && !other.wallId().equals(cabinet.getWallId())
					&& own.atSameHeightAs(other) && RoomLayout.overlap(own.polygon(), other.polygon(), TOLERANCE_MM))) {
				violations.add(cabinetViolation(CORNER_OVERLAP, cabinet));
				continue;
			}

			var template = templatesByCode.get(cabinet.getTemplateCode());
			if (template == null) {
				continue;
			}
			var blind = Math.max(0, template.getBlindWidthMm());
			var frontStart = cabinet.getOffsetMm() + (cabinet.effectiveBlindSide() == Side.LEFT ? blind : 0);
			var frontEnd = cabinet.getOffsetMm() + cabinet.getWidthMm()
					- (cabinet.effectiveBlindSide() == Side.RIGHT ? blind : 0);
			var strip = placement.rectangle(frontStart, frontEnd, cabinet.getDepthMm(),
					cabinet.getDepthMm() + FRONT_STRIP_MM);
			if (footprints.stream().anyMatch(other -> !other.wallId().equals(cabinet.getWallId())
					&& own.atSameHeightAs(other) && RoomLayout.overlap(strip, other.polygon(), TOLERANCE_MM))) {
				violations.add(cabinetViolation(FRONT_BLOCKED, cabinet));
			}
		}
	}

	/**
	 * A Cabinet's or Appliance's plan-view footprint and the heights it spans.
	 */
	private record Footprint(String id, String wallId, List<RoomLayout.Point> polygon, double elevationMm,
			double heightMm) {
		boolean atSameHeightAs(Footprint other) {
			return intersect(elevationMm, heightMm, other.elevationMm(), other.heightMm());
		}
	}

	/**
	 * Whether a Cabinet's face, seen straight on, overlaps a rectangle on its Wall.
	 */
	private static boolean covers(Cabinet cabinet, double offsetMm, double widthMm, double elevationMm,
			double heightMm) {
		return intersect(cabinet.getOffsetMm(), cabinet.getWidthMm(), offsetMm, widthMm)
				&& intersect(cabinet.getElevationMm(), cabinet.getHeightMm(), elevationMm, heightMm);
	}

	private static boolean sectionTooNarrow(Cabinet cabinet) {
		var previous = 0d;
		for (var position : cabinet.dividers()) {
			if (position == null || position - previous < MIN_SECTION_MM - TOLERANCE_MM) {
				return true;
			}
			previous = position;
		}
		return !cabinet.dividers().isEmpty() && cabinet.getWidthMm() - previous < MIN_SECTION_MM - TOLERANCE_MM;
	}

	private static boolean outsideWall(double offsetMm, double widthMm, double elevationMm, double heightMm,
			Wall wall) {
		return offsetMm < -TOLERANCE_MM || offsetMm + widthMm > wall.getLengthMm() + TOLERANCE_MM
				|| elevationMm < -TOLERANCE_MM || elevationMm + heightMm > wall.getHeightMm() + TOLERANCE_MM
				|| widthMm <= 0 || heightMm <= 0;
	}

	private static Violation itemViolation(String code, String wallId, String itemId) {
		return Violation.builder().code(code).wallId(wallId).itemId(itemId).build();
	}

	private static boolean overlap(Cabinet first, Cabinet second, Map<String, Wall> wallsById) {
		if (!wallsById.containsKey(first.getWallId()) || !first.getWallId().equals(second.getWallId())) {
			return false;
		}

		return intersect(first.getOffsetMm(), first.getWidthMm(), second.getOffsetMm(), second.getWidthMm())
				&& intersect(first.getElevationMm(), first.getHeightMm(), second.getElevationMm(),
						second.getHeightMm());
	}

	private static boolean intersect(double firstStart, double firstLength, double secondStart,
			double secondLength) {
		return Math.min(firstStart + firstLength, secondStart + secondLength)
				- Math.max(firstStart, secondStart) > TOLERANCE_MM;
	}

	private static boolean outside(double value, double min, double max) {
		return value < min - TOLERANCE_MM || value > max + TOLERANCE_MM;
	}

	private static Violation wallViolation(String code, Wall wall) {
		return Violation.builder().code(code).wallId(wall.getId()).build();
	}

	private static Violation cabinetViolation(String code, Cabinet cabinet) {
		return Violation.builder().code(code).wallId(cabinet.getWallId()).cabinetId(cabinet.getId()).build();
	}
}
