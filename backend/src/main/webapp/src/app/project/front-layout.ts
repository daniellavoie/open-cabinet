import { CabinetTemplate, FrontKind } from "./model/design";

/**
 * Sizes a Cabinet's front rows, as FrontLayout.java does for the Cut List, so
 * the 3D view draws the fronts that will be cut.
 */
export const DOUBLE_DOOR_MIN_WIDTH_MM = 609.6;

export interface SizedRow {
  kind: FrontKind;
  /** From the bottom of the box. */
  bottomMm: number;
  /** The front's own height, gaps already taken off. */
  heightMm: number;
}

export function frontRows(template: CabinetTemplate, boxHeightMm: number, gapMm: number): SizedRow[] {
  return sizeRows(template.frontRows ?? [], boxHeightMm, gapMm);
}

/**
 * The same for any rows, such as an assembled template's front stack.
 */
export function sizeRows(rows: { kind: FrontKind; heightMm?: number | null; weight?: number | null }[], boxHeightMm: number, gapMm: number): SizedRow[] {
  const weight = (row: { weight?: number | null }) => (row.weight ?? 0) > 0 ? row.weight! : 1;
  const available = boxHeightMm - gapMm * rows.length;
  const fixed = rows.filter(row => row.heightMm != null).reduce((sum, row) => sum + row.heightMm!, 0);
  const weights = rows.filter(row => row.heightMm == null).reduce((sum, row) => sum + weight(row), 0);
  const flexible = Math.max(0, available - fixed);

  const sized: SizedRow[] = [];
  let top = boxHeightMm;
  for (const row of rows) {
    const heightMm = row.heightMm != null ? row.heightMm : flexible * weight(row) / weights;
    top -= gapMm / 2;
    if (heightMm > 0) {
      sized.push({ kind: row.kind, bottomMm: top - heightMm, heightMm });
    }
    top -= heightMm + gapMm / 2;
  }
  return sized;
}

export function frontWidth(template: CabinetTemplate, cabinetWidthMm: number): number {
  return cabinetWidthMm - (template.blindWidthMm ?? 0);
}

export function doorLeaves(frontWidthMm: number): number {
  return frontWidthMm > DOUBLE_DOOR_MIN_WIDTH_MM ? 2 : 1;
}
