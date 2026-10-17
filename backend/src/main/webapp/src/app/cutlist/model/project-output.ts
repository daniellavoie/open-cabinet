import { MaterialKind } from "../../catalog/model/material";

export type PartRole = "SIDE" | "BOTTOM" | "TOP" | "STRETCHER" | "BACK" | "SHELF" | "DOOR" | "DRAWER_FRONT" | "FALSE_FRONT" | "BLIND_PANEL" | "DIVIDER" | "FINISHED_END" | "FILLER" | "TOE_KICK";

export class CutListRow {
  role!: PartRole;
  materialId!: string;
  materialName!: string;
  lengthMm!: number;
  widthMm!: number;
  thicknessMm!: number;
  grain!: boolean;
  bandedLengthEdges!: number;
  bandedWidthEdges!: number;
  edgeBandingId!: string | null;
  edgeBandingName!: string | null;
  quantity!: number;
  cabinetLabels!: string[];
}

export class SheetPlacement {
  cabinetLabel!: string;
  role!: PartRole;
  alongMm!: number;
  acrossMm!: number;
  lengthMm!: number;
  widthMm!: number;
  rotated!: boolean;
}

export class NestedSheet {
  placements!: SheetPlacement[];
  utilization!: number;
}

export class OversizedPart {
  cabinetLabel!: string;
  role!: PartRole;
  lengthMm!: number;
  widthMm!: number;
}

/**
 * How a layout was found: which way the first rip cuts run, which Parts go
 * first, and which way Parts without grain lie.
 */
export class NestingStrategy {
  stripDirection!: "ALONG" | "ACROSS";
  order!: "WIDTH" | "LENGTH" | "AREA";
  orientation!: "LONG_ALONG" | "LONG_ACROSS";
}

export class MaterialNesting {
  materialId!: string;
  materialName!: string;
  sheetLengthMm!: number;
  sheetWidthMm!: number;
  grain!: boolean;
  sheets!: NestedSheet[];
  oversizedParts!: OversizedPart[];
  strategy?: NestingStrategy;
  /** The next best layouts, best first, to compare; materials follow this one. */
  alternatives?: MaterialNesting[];
}

export class BomLine {
  materialId!: string;
  kind!: MaterialKind;
  name!: string;
  sku!: string | null;
  quantity!: number;
  unitPrice!: number;
  total!: number;
}

export class OperationCounts {
  sheets!: number;
  edgeBandingMeters!: number;
  cabinets!: number;
  hardwareItems!: number;
  linearFeet!: number;
}

/**
 * Identical Fronts to order from a supplier, in one style.
 */
export class OrderedFrontRow {
  role!: PartRole;
  materialId!: string;
  materialName!: string;
  heightMm!: number;
  widthMm!: number;
  quantity!: number;
  cabinetLabels!: string[];
}

export class ProjectOutput {
  cabinetLabels!: Record<string, string>;
  cutList!: CutListRow[];
  orderedFronts?: OrderedFrontRow[];
  nesting!: MaterialNesting[];
  billOfMaterials!: BomLine[];
  materialsTotal!: number;
  operations!: OperationCounts;
}
