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
