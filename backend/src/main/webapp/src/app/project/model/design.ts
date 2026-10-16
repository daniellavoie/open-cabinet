export type CabinetType = "BASE" | "WALL";

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
  drawerCount!: number;
  doors!: boolean;
}

export class Violation {
  code!: string;
  wallId!: string | null;
  cabinetId!: string | null;
}
