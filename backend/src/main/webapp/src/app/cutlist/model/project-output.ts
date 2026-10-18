import { MaterialKind } from "../../catalog/model/material";
import { NestingLayout } from "../../project/model/project";

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

/**
 * One saw cut, numbered in cutting order (1, 1.1, 1.1.1…), from the sheet's corner.
 */
export class SheetCut {
  number!: string;
  fromAlongMm!: number;
  fromAcrossMm!: number;
  toAlongMm!: number;
  toAcrossMm!: number;
}

export class NestedSheet {
  placements!: SheetPlacement[];
  utilization!: number;
  cuts!: SheetCut[];
}

/** What one layout would take for a material. */
export class NestingAlternative {
  layout!: NestingLayout;
  sheets!: number;
  cuts!: number;
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
  layout!: NestingLayout;
  sheets!: NestedSheet[];
  oversizedParts!: OversizedPart[];
  alternatives!: NestingAlternative[];
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

/** What can be printed or exported from a Project's output, as named in the API. */
export type OutputDocument = "cut-list" | "nesting" | "bill-of-materials";
