import { allowsDividers, Appliance, Cabinet, CabinetTemplate, Design, standsOnFloor, templateKey, Violation, Wall } from "./model/design";
import { layoutWalls, PlanPoint, polygonsOverlap, wallRectangle, WallPlacement } from "./room-layout";

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

/**
 * Depth of the strip in front of a Cabinet's fronts that nothing may stand in.
 */
const FRONT_STRIP_MM = 2;

/**
 * Narrowest section between dividers (or a divider and a side), centre to centre.
 */
export const MIN_SECTION_MM = 152.4;

export function checkConstructionRules(design: Design, templates: CabinetTemplate[]): Violation[] {
  const violations: Violation[] = [];
  const walls = design.walls ?? [];
  const cabinets = design.cabinets ?? [];
  const openings = design.openings ?? [];
  const appliances = design.appliances ?? [];
  const fillers = design.fillers ?? [];
  const wallsById = new Map<string, Wall>();
  walls.forEach(wall => {
    if (!wallsById.has(wall.id)) {
      wallsById.set(wall.id, wall);
    }
  });
  const templatesByKey = new Map(templates.map(template => [templateKey(template.code, template.version), template]));

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

  for (const filler of fillers) {
    const wall = wallsById.get(filler.wallId);
    if (!wall) {
      violations.push(itemViolation("UNKNOWN_WALL", filler.wallId, filler.id));
    } else if (outsideWall(filler, wall)) {
      violations.push(itemViolation("ITEM_OUTSIDE_WALL", filler.wallId, filler.id));
    }
  }

  for (const cabinet of cabinets) {
    const wall = wallsById.get(cabinet.wallId);
    if (!wall) {
      violations.push(cabinetViolation("UNKNOWN_WALL", cabinet));
      continue;
    }

    const template = templatesByKey.get(templateKey(cabinet.templateCode, cabinet.templateVersion));
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
    if ((cabinet.dividerPositionsMm ?? []).length > 0 && !allowsDividers(template)) {
      violations.push(cabinetViolation("DIVIDERS_NOT_ALLOWED", cabinet));
    } else if (sectionTooNarrow(cabinet)) {
      violations.push(cabinetViolation("SECTION_TOO_NARROW", cabinet));
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

  fillers.forEach((filler, index) => {
    const others: (Placed & { wallId: string })[] = [...cabinets, ...appliances, ...fillers.slice(0, index)];
    if (wallsById.has(filler.wallId) && others.some(other => other.wallId === filler.wallId && covers(other, filler))) {
      violations.push(itemViolation("OVERLAP", filler.wallId, filler.id));
    }
  });

  checkCorners(walls, cabinets, appliances, templatesByKey, violations);

  return violations;
}

interface Footprint {
  id: string;
  wallId: string;
  polygon: PlanPoint[];
  elevationMm: number;
  heightMm: number;
}

/**
 * Rules across Walls, in plan view. A Cabinet must not overlap a Cabinet or
 * Appliance on another Wall at the same height, nor have one standing right
 * in front of its fronts; the blind part of a blind corner Cabinet is meant to
 * be covered by the neighbouring run. On a single Wall, the overlap rules
 * above already cover both.
 */
function checkCorners(walls: Wall[], cabinets: Cabinet[], appliances: Appliance[], templatesByKey: Map<string, CabinetTemplate>, violations: Violation[]) {
  const placements = new Map<string, WallPlacement>();
  layoutWalls(walls).forEach(placement => {
    if (!placements.has(placement.wall.id)) {
      placements.set(placement.wall.id, placement);
    }
  });
  const footprints: Footprint[] = [];
  for (const item of [...cabinets, ...appliances]) {
    const placement = placements.get(item.wallId);
    if (placement) {
      footprints.push({
        id: item.id,
        wallId: item.wallId,
        polygon: wallRectangle(placement, item.offsetMm, item.offsetMm + item.widthMm, 0, item.depthMm),
        elevationMm: item.elevationMm,
        heightMm: item.heightMm,
      });
    }
  }
  const atSameHeight = (first: Footprint, second: Footprint) =>
    intersect(first.elevationMm, first.heightMm, second.elevationMm, second.heightMm);

  for (const cabinet of cabinets) {
    const placement = placements.get(cabinet.wallId);
    if (!placement) {
      continue;
    }
    const own = footprints.find(footprint => footprint.id === cabinet.id)!;
    if (footprints.some(other => other !== own && other.wallId !== cabinet.wallId
      && atSameHeight(own, other) && polygonsOverlap(own.polygon, other.polygon, TOLERANCE_MM))) {
      violations.push(cabinetViolation("CORNER_OVERLAP", cabinet));
      continue;
    }

    const template = templatesByKey.get(templateKey(cabinet.templateCode, cabinet.templateVersion));
    if (!template) {
      continue;
    }
    const blind = Math.max(0, template.blindWidthMm ?? 0);
    const blindSide = cabinet.mirrored ? "LEFT" : "RIGHT";
    const frontStart = cabinet.offsetMm + (blindSide === "LEFT" ? blind : 0);
    const frontEnd = cabinet.offsetMm + cabinet.widthMm - (blindSide === "RIGHT" ? blind : 0);
    const strip = wallRectangle(placement, frontStart, frontEnd, cabinet.depthMm, cabinet.depthMm + FRONT_STRIP_MM);
    if (footprints.some(other => other.wallId !== cabinet.wallId
      && atSameHeight(own, other) && polygonsOverlap(strip, other.polygon, TOLERANCE_MM))) {
      violations.push(cabinetViolation("FRONT_BLOCKED", cabinet));
    }
  }
}

/**
 * Whether two things on the same Wall overlap, seen straight on.
 */
function covers(first: Placed, second: Placed): boolean {
  return intersect(first.offsetMm, first.widthMm, second.offsetMm, second.widthMm)
    && intersect(first.elevationMm, first.heightMm, second.elevationMm, second.heightMm);
}

function sectionTooNarrow(cabinet: Cabinet): boolean {
  const dividers = cabinet.dividerPositionsMm ?? [];
  let previous = 0;
  for (const position of dividers) {
    if (typeof position !== "number" || position - previous < MIN_SECTION_MM - TOLERANCE_MM) {
      return true;
    }
    previous = position;
  }
  return dividers.length > 0 && cabinet.widthMm - previous < MIN_SECTION_MM - TOLERANCE_MM;
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
