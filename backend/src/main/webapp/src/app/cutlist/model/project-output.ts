import { MaterialKind } from "../../catalog/model/material";

export type PartRole = "SIDE" | "BOTTOM" | "TOP" | "STRETCHER" | "BACK" | "SHELF" | "DOOR" | "DRAWER_FRONT" | "FALSE_FRONT" | "BLIND_PANEL" | "DIVIDER";

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

export class MaterialNesting {
  materialId!: string;
  materialName!: string;
  sheetLengthMm!: number;
  sheetWidthMm!: number;
  grain!: boolean;
  sheets!: NestedSheet[];
  oversizedParts!: OversizedPart[];
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
