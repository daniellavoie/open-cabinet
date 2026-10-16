import { Cabinet, CabinetTemplate, Design, Violation, Wall } from "./model/design";

/**
 * The Construction Rules, run in the browser for live feedback while
 * designing. The server's ConstructionRules.java is the authority
 * (docs/adr/0002-construction-rules-run-in-browser-and-server.md); both must
 * return the same violations, in the same order, for every case in
 * backend/src/test/resources/construction-rules/cases.json.
 */
export const TOLERANCE_MM = 0.5;

export function checkConstructionRules(design: Design, templates: CabinetTemplate[]): Violation[] {
  const violations: Violation[] = [];
  const walls = design.walls ?? [];
  const cabinets = design.cabinets ?? [];
  const wallsById = new Map<string, Wall>();
  walls.forEach(wall => {
    if (!wallsById.has(wall.id)) {
      wallsById.set(wall.id, wall);
    }
  });
  const templatesByCode = new Map(templates.map(template => [template.code, template]));

  for (const wall of walls) {
    if (wall.lengthMm <= 0 || wall.heightMm <= 0) {
      violations.push({ code: "INVALID_WALL", wallId: wall.id, cabinetId: null });
    }
  }

  for (const cabinet of cabinets) {
    const wall = wallsById.get(cabinet.wallId);
    if (!wall) {
      violations.push(cabinetViolation("UNKNOWN_WALL", cabinet));
      continue;
    }

    const template = templatesByCode.get(cabinet.templateCode);
    if (!template) {
      violations.push(cabinetViolation("UNKNOWN_TEMPLATE", cabinet));
      continue;
    }

    if (outside(cabinet.widthMm, template.minWidthMm, template.maxWidthMm)) {
      violations.push(cabinetViolation("WIDTH_OUT_OF_RANGE", cabinet));
    }
    if (outside(cabinet.heightMm, template.minHeightMm, template.maxHeightMm)) {
      violations.push(cabinetViolation("HEIGHT_OUT_OF_RANGE", cabinet));
    }
    if (outside(cabinet.depthMm, template.minDepthMm, template.maxDepthMm)) {
      violations.push(cabinetViolation("DEPTH_OUT_OF_RANGE", cabinet));
    }
    if (cabinet.shelfCount < 0 || cabinet.shelfCount > template.maxShelfCount) {
      violations.push(cabinetViolation("SHELF_COUNT_OUT_OF_RANGE", cabinet));
    }
    if (template.type === "BASE" && Math.abs(cabinet.elevationMm) > TOLERANCE_MM) {
      violations.push(cabinetViolation("BASE_NOT_ON_FLOOR", cabinet));
    } else if (cabinet.elevationMm < -TOLERANCE_MM) {
      violations.push(cabinetViolation("BELOW_FLOOR", cabinet));
    }
    if (cabinet.offsetMm < -TOLERANCE_MM
      || cabinet.offsetMm + cabinet.widthMm > wall.lengthMm + TOLERANCE_MM) {
      violations.push(cabinetViolation("OUTSIDE_WALL", cabinet));
    }
    if (cabinet.elevationMm + cabinet.heightMm > wall.heightMm + TOLERANCE_MM) {
      violations.push(cabinetViolation("ABOVE_WALL", cabinet));
    }
  }

  for (let second = 0; second < cabinets.length; second++) {
    for (let first = 0; first < second; first++) {
      if (overlap(cabinets[first], cabinets[second], wallsById)) {
        violations.push(cabinetViolation("OVERLAP", cabinets[second]));
        break;
      }
    }
  }

  return violations;
}

function overlap(first: Cabinet, second: Cabinet, wallsById: Map<string, Wall>): boolean {
  if (!wallsById.has(first.wallId) || first.wallId !== second.wallId) {
    return false;
  }

  return intersect(first.offsetMm, first.widthMm, second.offsetMm, second.widthMm)
    && intersect(first.elevationMm, first.heightMm, second.elevationMm, second.heightMm);
}

function intersect(firstStart: number, firstLength: number, secondStart: number, secondLength: number): boolean {
  return Math.min(firstStart + firstLength, secondStart + secondLength)
    - Math.max(firstStart, secondStart) > TOLERANCE_MM;
}

function outside(value: number, min: number, max: number): boolean {
  return value < min - TOLERANCE_MM || value > max + TOLERANCE_MM;
}

function cabinetViolation(code: string, cabinet: Cabinet): Violation {
  return { code, wallId: cabinet.wallId, cabinetId: cabinet.id };
}
