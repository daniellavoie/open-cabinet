import { checkConstructionRules } from "./construction-rules";
import { Appliance, ApplianceKind, Cabinet, CabinetTemplate, Design, Filler, Opening, OpeningKind, Wall } from "./model/design";

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
 * Removes a Wall and everything on it. A Room keeps at least one Wall.
 */
export function removeWall(design: Design, wallId: string): Design {
  if (design.walls.length <= 1) {
    return design;
  }
  return {
    ...design,
    walls: design.walls.filter(wall => wall.id !== wallId),
    cabinets: design.cabinets.filter(cabinet => cabinet.wallId !== wallId),
    openings: (design.openings ?? []).filter(opening => opening.wallId !== wallId),
    appliances: (design.appliances ?? []).filter(appliance => appliance.wallId !== wallId),
    fillers: (design.fillers ?? []).filter(filler => filler.wallId !== wallId),
  };
}

const INCH = 25.4;
const CORNER_FILLER_MM = 1 * INCH;

export const OPENING_DEFAULTS: Record<OpeningKind, Omit<Opening, "id" | "kind" | "wallId" | "offsetMm">> = {
  WINDOW: { widthMm: 36 * INCH, heightMm: 42 * INCH, elevationMm: 42 * INCH },
  DOORWAY: { widthMm: 36 * INCH, heightMm: 80 * INCH, elevationMm: 0 },
};

export const APPLIANCE_DEFAULTS: Record<ApplianceKind, Omit<Appliance, "id" | "kind" | "wallId" | "offsetMm">> = {
  FRIDGE: { widthMm: 36 * INCH, heightMm: 70 * INCH, depthMm: 30 * INCH, elevationMm: 0 },
  RANGE: { widthMm: 30 * INCH, heightMm: 36 * INCH, depthMm: 26 * INCH, elevationMm: 0 },
  DISHWASHER: { widthMm: 24 * INCH, heightMm: 34.5 * INCH, depthMm: 24 * INCH, elevationMm: 0 },
  HOOD: { widthMm: 30 * INCH, heightMm: 18 * INCH, depthMm: 20 * INCH, elevationMm: 66 * INCH },
};

/**
 * A base-height Filler: as tall as a base Cabinet's box, above its toe kick.
 */
export const FILLER_DEFAULTS: Omit<Filler, "id" | "wallId" | "offsetMm"> = {
  widthMm: 3 * INCH, heightMm: 30.5 * INCH, depthMm: 24 * INCH, elevationMm: 4 * INCH,
};

export function addFiller(design: Design, wallId: string): { design: Design; itemId: string } {
  const filler: Filler = { id: newId("filler"), wallId, offsetMm: 0, ...FILLER_DEFAULTS };
  return { design: { ...design, fillers: [...(design.fillers ?? []), filler] }, itemId: filler.id };
}

export function addOpening(design: Design, kind: OpeningKind, wallId: string): { design: Design; itemId: string } {
  const opening: Opening = { id: newId("opening"), kind, wallId, offsetMm: 0, ...OPENING_DEFAULTS[kind] };
  return { design: { ...design, openings: [...(design.openings ?? []), opening] }, itemId: opening.id };
}

export function addAppliance(design: Design, kind: ApplianceKind, wallId: string): { design: Design; itemId: string } {
  const appliance: Appliance = { id: newId("appliance"), kind, wallId, offsetMm: 0, ...APPLIANCE_DEFAULTS[kind] };
  return { design: { ...design, appliances: [...(design.appliances ?? []), appliance] }, itemId: appliance.id };
}

/**
 * Changes an Opening, Appliance or Filler, whichever has this id.
 */
export type ItemChanges = Partial<Omit<Opening, "id" | "kind"> & Omit<Appliance, "id" | "kind"> & Omit<Filler, "id">>;

export function updateItem(design: Design, itemId: string, changes: ItemChanges): Design {
  return {
    ...design,
    openings: (design.openings ?? []).map(item => item.id === itemId ? { ...item, ...changes, id: item.id, kind: item.kind } as Opening : item),
    appliances: (design.appliances ?? []).map(item => item.id === itemId ? { ...item, ...changes, id: item.id, kind: item.kind } as Appliance : item),
    fillers: (design.fillers ?? []).map(item => item.id === itemId ? { ...item, ...changes, id: item.id } as Filler : item),
  };
}

export function removeItem(design: Design, itemId: string): Design {
  return {
    ...design,
    openings: (design.openings ?? []).filter(item => item.id !== itemId),
    appliances: (design.appliances ?? []).filter(item => item.id !== itemId),
    fillers: (design.fillers ?? []).filter(item => item.id !== itemId),
  };
}

/**
 * Adds a Cabinet from a template (with a Shop template's preset, if given) to
 * a Wall, at the first spot along the Wall
 * where it breaks no Construction Rule (or at the Wall's start if none):
 * the start, just past another Cabinet, Appliance, Opening or Filler, or past what
 * stands on a neighbouring Wall's corner, leaving room for a filler.
 */
export function addCabinet(design: Design, template: CabinetTemplate, wallId: string, templates: CabinetTemplate[],
  preset?: Omit<Cabinet, "id" | "wallId">): { design: Design; cabinetId: string } {
  const cabinet: Cabinet = {
    templateCode: template.code,
    offsetMm: 0,
    widthMm: template.defaultWidthMm,
    heightMm: template.defaultHeightMm,
    depthMm: template.defaultDepthMm,
    elevationMm: template.defaultElevationMm,
    shelfCount: template.defaultShelfCount,
    ...preset,
    id: newId("cabinet"),
    wallId,
  };

  const onWall = [...design.cabinets, ...design.appliances ?? [], ...design.openings ?? [], ...design.fillers ?? []]
    .filter(other => other.wallId === wallId)
    .map(other => other.offsetMm + other.widthMm);
  const pastCorner = [...design.cabinets, ...design.appliances ?? []]
    .filter(other => other.wallId !== wallId)
    .map(other => other.depthMm + CORNER_FILLER_MM);
  const candidates = [0, ...onWall, ...pastCorner]
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

/**
 * Adds a divider in the middle of the Cabinet's widest section.
 */
export function addDivider(design: Design, cabinetId: string): Design {
  return withDividers(design, cabinetId, (dividers, cabinet) => {
    const bounds = [0, ...dividers, cabinet.widthMm];
    let widest = 0;
    for (let index = 1; index < bounds.length - 1; index++) {
      if (bounds[index + 1] - bounds[index] > bounds[widest + 1] - bounds[widest]) {
        widest = index;
      }
    }
    const middle = Math.round((bounds[widest] + bounds[widest + 1]) / 2 * 10) / 10;
    return [...dividers.slice(0, widest), middle, ...dividers.slice(widest)];
  });
}

export function moveDivider(design: Design, cabinetId: string, index: number, positionMm: number): Design {
  return withDividers(design, cabinetId, dividers => dividers.map((position, at) => at === index ? positionMm : position));
}

export function removeDivider(design: Design, cabinetId: string, index: number): Design {
  return withDividers(design, cabinetId, dividers => dividers.filter((_, at) => at !== index));
}

function withDividers(design: Design, cabinetId: string, change: (dividers: number[], cabinet: Cabinet) => number[]): Design {
  return {
    ...design,
    cabinets: design.cabinets.map(cabinet => cabinet.id === cabinetId
      ? { ...cabinet, dividerPositionsMm: change(cabinet.dividerPositionsMm ?? [], cabinet) }
      : cabinet),
  };
}

export function removeCabinet(design: Design, cabinetId: string): Design {
  return { ...design, cabinets: design.cabinets.filter(cabinet => cabinet.id !== cabinetId) };
}
