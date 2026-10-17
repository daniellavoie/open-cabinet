package dev.daniellavoie.opencabinet.project.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.Design;
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

	/**
	 * Slack allowed on every comparison, in millimetres, so values rounded for display still pass.
	 */
	static final double TOLERANCE_MM = 0.5;

	private ConstructionRules() {
	}

	public static List<Violation> check(Design design, List<CabinetTemplate> templates) {
		var violations = new ArrayList<Violation>();
		var walls = Optional.ofNullable(design.getWalls()).orElse(List.of());
		var cabinets = Optional.ofNullable(design.getCabinets()).orElse(List.of());
		Map<String, Wall> wallsById = walls.stream()
				.collect(Collectors.toMap(Wall::getId, Function.identity(), (first, second) -> first));
		Map<String, CabinetTemplate> templatesByCode = templates.stream()
				.collect(Collectors.toMap(CabinetTemplate::getCode, Function.identity()));

		for (var wall : walls) {
			if (wall.getLengthMm() <= 0 || wall.getHeightMm() <= 0) {
				violations.add(wallViolation(INVALID_WALL, wall));
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
		}

		for (int second = 0; second < cabinets.size(); second++) {
			for (int first = 0; first < second; first++) {
				if (overlap(cabinets.get(first), cabinets.get(second), wallsById)) {
					violations.add(cabinetViolation(OVERLAP, cabinets.get(second)));
					break;
				}
			}
		}

		return violations;
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
