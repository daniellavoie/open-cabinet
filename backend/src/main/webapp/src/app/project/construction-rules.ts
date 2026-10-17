import { Cabinet, CabinetTemplate, Design, standsOnFloor, Violation, Wall } from "./model/design";

interface Placed {
  offsetMm: number;
  widthMm: number;
  elevationMm: number;
  heightMm: number;
}

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
  const openings = design.openings ?? [];
  const appliances = design.appliances ?? [];
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

  for (const opening of openings) {
    const wall = wallsById.get(opening.wallId);
    if (!wall) {
      violations.push(itemViolation("UNKNOWN_WALL", opening.wallId, opening.id));
    } else if (outsideWall(opening, wall)) {
      violations.push(itemViolation("ITEM_OUTSIDE_WALL", opening.wallId, opening.id));
    }
  }

  for (const appliance of appliances) {
    const wall = wallsById.get(appliance.wallId);
    if (!wall) {
      violations.push(itemViolation("UNKNOWN_WALL", appliance.wallId, appliance.id));
    } else if (outsideWall(appliance, wall)) {
      violations.push(itemViolation("ITEM_OUTSIDE_WALL", appliance.wallId, appliance.id));
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
    if (standsOnFloor(template.type) && Math.abs(cabinet.elevationMm) > TOLERANCE_MM) {
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
    if (openings.some(opening => opening.wallId === cabinet.wallId && covers(cabinet, opening))) {
      violations.push(cabinetViolation("BLOCKS_OPENING", cabinet));
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

  for (const cabinet of cabinets) {
    if (wallsById.has(cabinet.wallId)
      && appliances.some(appliance => appliance.wallId === cabinet.wallId && covers(cabinet, appliance))) {
      violations.push(cabinetViolation("OVERLAPS_APPLIANCE", cabinet));
    }
  }

  for (let second = 0; second < appliances.length; second++) {
    const later = appliances[second];
    for (let first = 0; first < second; first++) {
      const earlier = appliances[first];
      if (wallsById.has(later.wallId) && earlier.wallId === later.wallId && covers(earlier, later)) {
        violations.push(itemViolation("OVERLAP", later.wallId, later.id));
        break;
      }
    }
  }

  return violations;
}

/**
 * Whether two things on the same Wall overlap, seen straight on.
 */
function covers(first: Placed, second: Placed): boolean {
  return intersect(first.offsetMm, first.widthMm, second.offsetMm, second.widthMm)
    && intersect(first.elevationMm, first.heightMm, second.elevationMm, second.heightMm);
}

function outsideWall(item: Placed, wall: Wall): boolean {
  return item.offsetMm < -TOLERANCE_MM || item.offsetMm + item.widthMm > wall.lengthMm + TOLERANCE_MM
    || item.elevationMm < -TOLERANCE_MM || item.elevationMm + item.heightMm > wall.heightMm + TOLERANCE_MM
    || item.widthMm <= 0 || item.heightMm <= 0;
}

function itemViolation(code: string, wallId: string, itemId: string): Violation {
  return { code, wallId, cabinetId: null, itemId };
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
