import { Cabinet, Wall } from "./model/design";

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
 * Plan-view centre of a Cabinet's footprint.
 */
export function cabinetCentre(cabinet: Cabinet, placement: WallPlacement): { x: number; z: number } {
  const along = cabinet.offsetMm + cabinet.widthMm / 2;
  const out = cabinet.depthMm / 2;
  return {
    x: placement.start.x + placement.direction.x * along + placement.normal.x * out,
    z: placement.start.z + placement.direction.z * along + placement.normal.z * out,
  };
}

/**
 * Snaps an offset along a Wall to its ends and to the sides of the other
 * Cabinets on it, when within `snapMm`, otherwise to the `stepMm` grid.
 */
export function snapOffset(cabinet: Cabinet, offsetMm: number, wall: Wall, others: Cabinet[], stepMm: number, snapMm: number): number {
  const candidates = [0, wall.lengthMm - cabinet.widthMm];
  for (const other of others) {
    if (other.id !== cabinet.id && other.wallId === cabinet.wallId) {
      candidates.push(other.offsetMm + other.widthMm, other.offsetMm - cabinet.widthMm);
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
