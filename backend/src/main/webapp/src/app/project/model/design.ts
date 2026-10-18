import type { PartRole } from "../../cutlist/model/project-output";

export type CabinetType = "BASE" | "WALL" | "TALL";

export type FrontKind = "DOOR" | "DRAWER" | "FALSE_FRONT" | "OPENING";

export type Side = "LEFT" | "RIGHT";

export type FrontSource = "MADE" | "ORDERED";

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
  /** The Template Version the Cabinet keeps until a Member updates or reverts it. */
  templateVersion!: number;
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
  /**
   * Vertical dividers, as the distance from the Cabinet's left side to each
   * divider's centre, left to right. Each section gets `shelfCount` shelves.
   */
  dividerPositionsMm?: number[] | null;
  /** Made in the Shop when unset. */
  frontSource?: FrontSource | null;
  /** The ordered Front style (a FRONT catalog entry), when not the Construction Standard's. */
  orderedFrontId?: string | null;
  /** Finished end panels applied over an exposed side. */
  finishedLeft?: boolean;
  finishedRight?: boolean;
  /** Crown moulding along the top front, for wall and tall Cabinets. */
  crown?: boolean;
}

export type OpeningKind = "WINDOW" | "DOORWAY";

export type ApplianceKind = "FRIDGE" | "RANGE" | "DISHWASHER" | "HOOD";

/**
 * A window or doorway in a Wall that Cabinets must not cover.
 */
export class Opening {
  id!: string;
  kind!: OpeningKind;
  wallId!: string;
  offsetMm!: number;
  widthMm!: number;
  elevationMm!: number;
  heightMm!: number;
}

/**
 * A placeholder for an appliance that Cabinets are fitted around.
 */
export class Appliance {
  id!: string;
  kind!: ApplianceKind;
  wallId!: string;
  offsetMm!: number;
  widthMm!: number;
  heightMm!: number;
  depthMm!: number;
  elevationMm!: number;
}

/**
 * A strip of Front material closing a gap between Cabinets, or between a
 * Cabinet and a Wall or Appliance. Its face stands `depthMm` out from the
 * Wall, flush with the Fronts beside it.
 */
export class Filler {
  id!: string;
  wallId!: string;
  offsetMm!: number;
  widthMm!: number;
  heightMm!: number;
  depthMm!: number;
  elevationMm!: number;
}

export class Design {
  walls!: Wall[];
  cabinets!: Cabinet[];
  openings?: Opening[];
  appliances?: Appliance[];
  fillers?: Filler[];
}

/**
 * One Template Version of a kind of Cabinet. A published version never
 * changes; `changeNote` says what changed from the previous one.
 */
export class CabinetTemplate {
  code!: string;
  /** Typed once by the Member for a template they designed; the starter library's come from the app. */
  name?: string | null;
  version!: number;
  changeNote?: string | null;
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
  /**
   * The template's Parts held by Attachments (ADR 0006); without one, the
   * fixed recipe of the starter library's version 1 builds it.
   */
  assembly?: TemplateAssembly | null;
}

/**
 * Identifies a Template Version: its template's code and its number.
 */
export function templateKey(code: string, version: number): string {
  return `${code}@${version}`;
}

/**
 * Dividers fit behind doors only: drawers, false fronts and appliance
 * openings span the width.
 */
export function allowsDividers(template: CabinetTemplate): boolean {
  if (template.assembly) {
    return !!template.assembly.divider;
  }
  return template.frontRows.length > 0 && template.frontRows.every(row => row.kind === "DOOR");
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
  /** The Opening or Appliance, for violations about one. */
  itemId?: string | null;
}

// Assembled templates (docs/adr/0006-template-is-an-assembly-of-parts.md), in the
// Cabinet's frame: x from its left side, y up from its bottom, z from its back.

export type Axis = "X" | "Y" | "Z";

/** LEFT and RIGHT across x, BOTTOM and TOP across y, BACK and FRONT across z. */
export type Face = "LEFT" | "RIGHT" | "BOTTOM" | "TOP" | "BACK" | "FRONT";

export type AssemblyPartKind = "PANEL" | "MOULDING";

export type StandardValue = "DOOR_GAP" | "TOE_KICK_HEIGHT";

export type MaterialRole = "CARCASS" | "BACK" | "FRONT" | "CARCASS_BANDING" | "FRONT_BANDING" | "CROWN_MOULDING"
  | "HINGE" | "SHELF_PIN" | "LEG" | "PULL" | "DRAWER_SLIDE" | "DRAWER_BOX";

/** A Construction Standard role, or a specific catalog material. */
export class MaterialRef {
  role?: MaterialRole | null;
  materialId?: string | null;
}

/** Millimetres, a Standard value or a material's thickness, times `factor` (1 when unset). */
export class Term {
  mm?: number | null;
  standard?: StandardValue | null;
  thickness?: MaterialRef | null;
  factor?: number | null;
}

/** A face held at a face of the Cabinet (`to` = CABINET) or of the Part named `to`, moved by `offset`. */
export class Attachment {
  to!: string;
  face!: Face;
  offset?: Term[] | null;
}

export class Banding {
  faces!: Face[];
  material!: MaterialRef;
}

export class AssemblyPart {
  id!: string;
  role!: PartRole;
  name?: string | null;
  kind?: AssemblyPartKind | null;
  material!: MaterialRef;
  thickness?: Axis | null;
  grain?: Axis | null;
  faces?: Partial<Record<Face, Attachment>> | null;
  sizes?: Partial<Record<Axis, Term[]>> | null;
  repeat?: { count: "SHELVES"; dividerGapMm?: number | null } | null;
  banding?: Banding | null;
  /** A Front outside any front stack, such as a blind corner's panel: ordered with the Cabinet's Fronts. */
  front?: boolean | null;
}

export class StackRow {
  kind!: FrontKind;
  heightMm?: number | null;
  weight?: number | null;
}

export class FrontStack {
  id!: string;
  material!: MaterialRef;
  faces?: Partial<Record<Face, Attachment>> | null;
  rows?: StackRow[] | null;
  splitAboveMm?: number | null;
}

export class Hardware {
  material!: MaterialRef;
  on?: string | null;
  frontKind?: FrontKind | null;
  /** Per host: fixed, one per `perMm` along `axis` (at least `minimum`), or by `steps` of its size along `axis`. */
  quantity!: {
    fixed?: number | null; perMm?: number | null; axis?: Axis | null; minimum?: number | null;
    steps?: { upToMm?: number | null; quantity: number }[] | null;
  };
}

export class TemplateAssembly {
  parts?: AssemblyPart[] | null;
  frontStacks?: FrontStack[] | null;
  divider?: AssemblyPart | null;
  hardware?: Hardware[] | null;
}
