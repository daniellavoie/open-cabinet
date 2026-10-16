import { checkConstructionRules } from "./construction-rules";
import { Cabinet, CabinetTemplate, Design, Wall } from "./model/design";

/**
 * Immutable edits to a Design. Each returns a new Design; the editor checks
 * the Construction Rules on the result.
 */
export function newId(prefix: string): string {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

export function addWall(design: Design): Design {
  const last = design.walls[design.walls.length - 1];
  const wall: Wall = {
    id: newId("wall"),
    lengthMm: last?.lengthMm ?? 3657.6,
    heightMm: last?.heightMm ?? 2438.4,
    angleToNextDegrees: 90,
  };
  return { ...design, walls: [...design.walls, wall] };
}

export function updateWall(design: Design, wallId: string, changes: Partial<Wall>): Design {
  return { ...design, walls: design.walls.map(wall => wall.id === wallId ? { ...wall, ...changes, id: wall.id } : wall) };
}

/**
 * Removes a Wall and the Cabinets on it. A Room keeps at least one Wall.
 */
export function removeWall(design: Design, wallId: string): Design {
  if (design.walls.length <= 1) {
    return design;
  }
  return {
    walls: design.walls.filter(wall => wall.id !== wallId),
    cabinets: design.cabinets.filter(cabinet => cabinet.wallId !== wallId),
  };
}

/**
 * Adds a Cabinet from a template to a Wall, at the first spot along the Wall
 * where it breaks no Construction Rule (or at the Wall's start if none).
 */
export function addCabinet(design: Design, template: CabinetTemplate, wallId: string, templates: CabinetTemplate[]): { design: Design; cabinetId: string } {
  const cabinet: Cabinet = {
    id: newId("cabinet"),
    templateCode: template.code,
    wallId,
    offsetMm: 0,
    widthMm: template.defaultWidthMm,
    heightMm: template.defaultHeightMm,
    depthMm: template.defaultDepthMm,
    elevationMm: template.defaultElevationMm,
    shelfCount: template.defaultShelfCount,
  };

  const candidates = [0, ...design.cabinets
    .filter(other => other.wallId === wallId)
    .map(other => other.offsetMm + other.widthMm)]
    .sort((first, second) => first - second);

  for (const offsetMm of candidates) {
    const placed = { ...cabinet, offsetMm };
    const candidate = { ...design, cabinets: [...design.cabinets, placed] };
    if (!checkConstructionRules(candidate, templates).some(violation => violation.cabinetId === placed.id)) {
      return { design: candidate, cabinetId: placed.id };
    }
  }

  return { design: { ...design, cabinets: [...design.cabinets, cabinet] }, cabinetId: cabinet.id };
}

export function updateCabinet(design: Design, cabinetId: string, changes: Partial<Cabinet>): Design {
  return {
    ...design,
    cabinets: design.cabinets.map(cabinet => cabinet.id === cabinetId ? { ...cabinet, ...changes, id: cabinet.id } : cabinet),
  };
}

export function removeCabinet(design: Design, cabinetId: string): Design {
  return { ...design, cabinets: design.cabinets.filter(cabinet => cabinet.id !== cabinetId) };
}
