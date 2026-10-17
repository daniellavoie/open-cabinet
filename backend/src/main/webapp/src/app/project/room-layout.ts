import { Wall } from "./model/design";

/**
 * Anything placed along a Wall: a Cabinet, Opening or Appliance.
 */
export interface OnWall {
  id: string;
  wallId: string;
  offsetMm: number;
  widthMm: number;
}

/**
 * Where each Wall sits in plan view, in millimetres. The first Wall starts at
 * the origin and runs along +x; each next Wall turns by the previous Wall's
 * angleToNextDegrees. Cabinets stand on the left of a Wall's direction, the
 * side its `normal` points to.
 */
export interface WallPlacement {
  wall: Wall;
  start: { x: number; z: number };
  direction: { x: number; z: number };
  normal: { x: number; z: number };
  angleRadians: number;
}

export function layoutWalls(walls: Wall[]): WallPlacement[] {
  const placements: WallPlacement[] = [];
  let x = 0;
  let z = 0;
  let angle = 0;

  for (const wall of walls) {
    const direction = { x: Math.cos(angle), z: Math.sin(angle) };
    placements.push({
      wall,
      start: { x, z },
      direction,
      normal: { x: -direction.z, z: direction.x },
      angleRadians: angle,
    });
    x += direction.x * wall.lengthMm;
    z += direction.z * wall.lengthMm;
    angle += (wall.angleToNextDegrees * Math.PI) / 180;
  }

  return placements;
}

export interface PlanPoint {
  x: number;
  z: number;
}

/**
 * The plan-view rectangle spanning `alongStart..alongEnd` along a Wall and
 * `outStart..outEnd` out from it.
 */
export function wallRectangle(placement: WallPlacement, alongStart: number, alongEnd: number, outStart: number, outEnd: number): PlanPoint[] {
  const at = (along: number, out: number) => ({
    x: placement.start.x + placement.direction.x * along + placement.normal.x * out,
    z: placement.start.z + placement.direction.z * along + placement.normal.z * out,
  });
  return [at(alongStart, outStart), at(alongEnd, outStart), at(alongEnd, outEnd), at(alongStart, outEnd)];
}

/**
 * Whether two convex polygons overlap by more than `toleranceMm` (separating
 * axis test), so polygons that only touch do not.
 */
export function polygonsOverlap(first: PlanPoint[], second: PlanPoint[], toleranceMm: number): boolean {
  return separatedOnNoAxisOf(first, first, second, toleranceMm) && separatedOnNoAxisOf(second, first, second, toleranceMm);
}

function separatedOnNoAxisOf(edges: PlanPoint[], first: PlanPoint[], second: PlanPoint[], toleranceMm: number): boolean {
  for (let index = 0; index < edges.length; index++) {
    const from = edges[index];
    const to = edges[(index + 1) % edges.length];
    const length = Math.hypot(to.x - from.x, to.z - from.z);
    if (length === 0) {
      continue;
    }
    const axis = { x: -(to.z - from.z) / length, z: (to.x - from.x) / length };
    const [firstMin, firstMax] = project(first, axis);
    const [secondMin, secondMax] = project(second, axis);
    if (Math.min(firstMax, secondMax) - Math.max(firstMin, secondMin) <= toleranceMm) {
      return false;
    }
  }
  return true;
}

function project(polygon: PlanPoint[], axis: PlanPoint): [number, number] {
  const values = polygon.map(point => point.x * axis.x + point.z * axis.z);
  return [Math.min(...values), Math.max(...values)];
}

/**
 * Plan-view centre of a footprint `depthMm` deep, standing against the Wall.
 */
export function footprintCentre(item: OnWall, depthMm: number, placement: WallPlacement): { x: number; z: number } {
  const along = item.offsetMm + item.widthMm / 2;
  const out = depthMm / 2;
  return {
    x: placement.start.x + placement.direction.x * along + placement.normal.x * out,
    z: placement.start.z + placement.direction.z * along + placement.normal.z * out,
  };
}

/**
 * Snaps an offset along a Wall to its ends and to the sides of the other
 * things on it, when within `snapMm`, otherwise to the `stepMm` grid.
 */
export function snapOffset(item: OnWall, offsetMm: number, wall: Wall, others: OnWall[], stepMm: number, snapMm: number): number {
  const candidates = [0, wall.lengthMm - item.widthMm];
  for (const other of others) {
    if (other.id !== item.id && other.wallId === item.wallId) {
      candidates.push(other.offsetMm + other.widthMm, other.offsetMm - item.widthMm);
    }
  }

  let best = Math.round(offsetMm / stepMm) * stepMm;
  let bestDistance = snapMm;
  for (const candidate of candidates) {
    const distance = Math.abs(candidate - offsetMm);
    if (distance < bestDistance) {
      best = candidate;
      bestDistance = distance;
    }
  }
  return Math.round(best * 10) / 10;
}
