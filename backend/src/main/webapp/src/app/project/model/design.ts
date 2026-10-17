export type CabinetType = "BASE" | "WALL" | "TALL";

export type FrontKind = "DOOR" | "DRAWER" | "FALSE_FRONT" | "OPENING";

export type Side = "LEFT" | "RIGHT";

/**
 * One row of a Cabinet's front, from the top: a fixed height, or a share
 * (weight, 1 when unset) of the height left by the fixed rows.
 */
export class FrontRow {
  kind!: FrontKind;
  heightMm?: number | null;
  weight?: number;
}

export class Wall {
  id!: string;
  lengthMm!: number;
  heightMm!: number;
  angleToNextDegrees!: number;
}

export class Cabinet {
  id!: string;
  templateCode!: string;
  wallId!: string;
  offsetMm!: number;
  widthMm!: number;
  heightMm!: number;
  depthMm!: number;
  elevationMm!: number;
  shelfCount!: number;
  /**
   * Which side of a blind corner Cabinet has no fronts; right when unset.
   */
  blindSide?: Side | null;
}

export class Design {
  walls!: Wall[];
  cabinets!: Cabinet[];
}

export class CabinetTemplate {
  code!: string;
  type!: CabinetType;
  defaultWidthMm!: number;
  minWidthMm!: number;
  maxWidthMm!: number;
  defaultHeightMm!: number;
  minHeightMm!: number;
  maxHeightMm!: number;
  defaultDepthMm!: number;
  minDepthMm!: number;
  maxDepthMm!: number;
  defaultElevationMm!: number;
  defaultShelfCount!: number;
  maxShelfCount!: number;
  frontRows!: FrontRow[];
  blindWidthMm?: number;
}

/**
 * Base and tall Cabinets stand on legs behind a toe kick.
 */
export function standsOnFloor(type: CabinetType): boolean {
  return type !== "WALL";
}

export class Violation {
  code!: string;
  wallId!: string | null;
  cabinetId!: string | null;
}
