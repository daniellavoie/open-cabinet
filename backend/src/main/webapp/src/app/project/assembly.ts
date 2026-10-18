import { Material } from "../catalog/model/material";
import type { PartRole } from "../cutlist/model/project-output";
import { ConstructionStandard } from "../shop/model/shop";
import { sizeRows } from "./front-layout";
import {
  AssemblyPart, AssemblyPartKind, Attachment, Axis, Banding, Cabinet, CabinetTemplate, Face, FrontKind, FrontStack, Hardware,
  MaterialRef, MaterialRole, StandardValue, TemplateAssembly, Term,
} from "./model/design";

/**
 * Builds a Cabinet from an assembled template (docs/adr/0006-template-is-an-assembly-of-parts.md),
 * as Assembly.java does for the Cut List: resolves every Attachment at the
 * Cabinet's size into boxes, repeats Parts, places dividers, lays out front
 * stacks and counts hardware. Kept in step by template-assembly/cases.json.
 */

export const TOLERANCE_MM = 0.01;
export const CABINET = "CABINET";

const AXES: Axis[] = ["X", "Y", "Z"];
const FACES: Record<Face, { axis: Axis; max: boolean }> = {
  LEFT: { axis: "X", max: false },
  RIGHT: { axis: "X", max: true },
  BOTTOM: { axis: "Y", max: false },
  TOP: { axis: "Y", max: true },
  BACK: { axis: "Z", max: false },
  FRONT: { axis: "Z", max: true },
};

export function faceOf(axis: Axis, max: boolean): Face {
  return (Object.keys(FACES) as Face[]).find(face => FACES[face].axis === axis && FACES[face].max === max)!;
}

export function faceAxis(face: Face): Axis {
  return FACES[face].axis;
}

/** A Cabinet's size and choices; `mirrored` builds the template left for right. */
export interface AssemblyContext {
  width: number;
  height: number;
  depth: number;
  shelfCount: number;
  dividers: number[];
  standard: Partial<Record<StandardValue, number>>;
  thickness: (material: MaterialRef) => number;
  mirrored?: boolean;
}

export interface Box {
  x0: number; x1: number; y0: number; y1: number; z0: number; z1: number;
}

export function boxMin(box: Box, axis: Axis): number {
  return axis === "X" ? box.x0 : axis === "Y" ? box.y0 : box.z0;
}

export function boxMax(box: Box, axis: Axis): number {
  return axis === "X" ? box.x1 : axis === "Y" ? box.y1 : box.z1;
}

export function boxSize(box: Box, axis: Axis): number {
  return boxMax(box, axis) - boxMin(box, axis);
}

/** A Part of the built Cabinet; `source` is the template Part or front stack it comes from. */
export interface AssembledPart {
  id: string;
  source: string;
  role: PartRole;
  name: string | null;
  kind: AssemblyPartKind;
  front: boolean;
  material: MaterialRef;
  box: Box;
  thickness: Axis | null;
  grain: Axis;
  banding: Banding | null;
}

export interface AssembledHardware {
  material: MaterialRef;
  quantity: number;
}

export interface AssemblyViolation {
  code: "UNKNOWN_TARGET" | "WRONG_ATTACHMENTS" | "UNRESOLVED";
  partId: string;
}

export interface AssemblyResult {
  parts: AssembledPart[];
  hardware: AssembledHardware[];
  violations: AssemblyViolation[];
}

type Extents = Map<string, Map<Axis, [number, number]>>;

export function assemble(assembly: TemplateAssembly, context: AssemblyContext): AssemblyResult {
  const violations: AssemblyViolation[] = [];
  const definitions = new Map<string, AssemblyPart>();
  for (const part of assembly.parts ?? []) {
    definitions.set(part.id, part);
  }

  for (const part of definitions.values()) {
    checkAttachments(part, !!part.repeat, false, definitions, violations);
  }
  if (assembly.divider) {
    checkAttachments(assembly.divider, false, true, definitions, violations);
  }
  if (violations.length > 0) {
    return { parts: [], hardware: [], violations };
  }

  const extents: Extents = new Map();
  for (const id of definitions.keys()) {
    extents.set(id, new Map());
  }
  let progress = true;
  while (progress) {
    progress = false;
    for (const part of definitions.values()) {
      for (const axis of AXES) {
        if (!extents.get(part.id)!.has(axis)) {
          const resolved = resolve(part, axis, !!part.repeat, extents, context);
          if (resolved) {
            extents.get(part.id)!.set(axis, resolved);
            progress = true;
          }
        }
      }
    }
  }
  for (const part of definitions.values()) {
    if (extents.get(part.id)!.size < AXES.length) {
      violations.push({ code: "UNRESOLVED", partId: part.id });
    }
  }
  if (violations.length > 0) {
    return { parts: [], hardware: [], violations };
  }

  const dividerParts: AssembledPart[] = [];
  const divider = assembly.divider;
  if (divider) {
    const thickness = context.thickness(divider.material);
    // Divider positions are the Cabinet's; a mirrored template is built as drawn, then flipped.
    const dividers = (context.dividers ?? []).map(position => context.mirrored ? context.width - position : position);
    for (let index = 0; index < dividers.length; index++) {
      const dividerExtents = new Map<Axis, [number, number]>();
      dividerExtents.set("X", [dividers[index] - thickness / 2, dividers[index] + thickness / 2]);
      for (const axis of ["Y", "Z"] as Axis[]) {
        const resolved = resolve(divider, axis, false, extents, context);
        if (!resolved) {
          violations.push({ code: "UNRESOLVED", partId: divider.id });
          return { parts: [], hardware: [], violations };
        }
        dividerExtents.set(axis, resolved);
      }
      dividerParts.push(assembled(divider, `${divider.id}#${index + 1}`, boxOf(dividerExtents)));
    }
  }

  const parts: AssembledPart[] = [];
  for (const part of definitions.values()) {
    const box = boxOf(extents.get(part.id)!);
    if (!part.repeat) {
      parts.push(assembled(part, part.id, box));
    } else {
      parts.push(...copies(part, box, Math.max(0, context.shelfCount), context.thickness(part.material), dividerParts));
    }
  }
  parts.push(...dividerParts);

  for (const stack of assembly.frontStacks ?? []) {
    const stackFronts = fronts(stack, extents, context);
    if (!stackFronts) {
      violations.push({ code: "UNRESOLVED", partId: stack.id });
    } else {
      parts.push(...stackFronts);
    }
  }

  const built = context.mirrored ? parts.map(part => mirror(part, context.width)) : parts;
  return { parts: built, hardware: hardware(assembly, built, context), violations };
}

/**
 * On each axis a Part needs exactly two of: its low face, its high face, its
 * size. A panel's size on its thickness axis is its material's thickness; a
 * repeated Part attaches both faces of that axis. A divider is placed across x
 * by the Cabinet, so its LEFT and RIGHT stay free.
 */
function checkAttachments(part: AssemblyPart, repeated: boolean, divider: boolean, definitions: Map<string, AssemblyPart>,
  violations: AssemblyViolation[]) {
  const faces = part.faces ?? {};
  for (const [face, attachment] of Object.entries(faces) as [Face, Attachment][]) {
    if (attachment.to !== CABINET) {
      const target = definitions.get(attachment.to);
      if (!target) {
        violations.push({ code: "UNKNOWN_TARGET", partId: part.id });
        return;
      }
      if (target.repeat || faceAxis(attachment.face) !== faceAxis(face)) {
        violations.push({ code: "WRONG_ATTACHMENTS", partId: part.id });
        return;
      }
    }
  }

  const panel = (part.kind ?? "PANEL") === "PANEL";
  if (panel && !part.thickness) {
    violations.push({ code: "WRONG_ATTACHMENTS", partId: part.id });
    return;
  }
  for (const axis of AXES) {
    if (divider && axis === "X") {
      if (faces.LEFT || faces.RIGHT || part.thickness !== "X") {
        violations.push({ code: "WRONG_ATTACHMENTS", partId: part.id });
        return;
      }
      continue;
    }
    const given = (faces[faceOf(axis, false)] ? 1 : 0) + (faces[faceOf(axis, true)] ? 1 : 0);
    const sized = !!part.sizes?.[axis] || (panel && axis === part.thickness && !repeated);
    if (repeated && axis === part.thickness ? given !== 2 : given + (sized ? 1 : 0) !== 2) {
      violations.push({ code: "WRONG_ATTACHMENTS", partId: part.id });
      return;
    }
  }
}

function resolve(part: AssemblyPart, axis: Axis, repeated: boolean, extents: Extents, context: AssemblyContext): [number, number] | null {
  const faces = part.faces ?? {};
  const low = faces[faceOf(axis, false)];
  const high = faces[faceOf(axis, true)];
  const lowValue = low ? position(low, extents, context) : null;
  const highValue = high ? position(high, extents, context) : null;
  if ((low && lowValue === null) || (high && highValue === null)) {
    return null;
  }
  if (repeated && axis === part.thickness) {
    return [lowValue!, highValue!];
  }
  if (lowValue !== null && highValue !== null) {
    return [lowValue, highValue];
  }
  const terms = part.sizes?.[axis];
  const size = terms ? sum(terms, context) : context.thickness(part.material);
  return lowValue !== null ? [lowValue, lowValue + size] : [highValue! - size, highValue!];
}

/** Where an attached face lies; null while its target is not placed on that axis yet. */
function position(attachment: Attachment, extents: Extents, context: AssemblyContext): number | null {
  const { axis, max } = FACES[attachment.face];
  let base: number;
  if (attachment.to === CABINET) {
    base = !max ? 0 : axis === "X" ? context.width : axis === "Y" ? context.height : context.depth;
  } else {
    const target = extents.get(attachment.to)?.get(axis);
    if (!target) {
      return null;
    }
    base = max ? target[1] : target[0];
  }
  return base + sum(attachment.offset ?? [], context);
}

export function sum(terms: Term[], context: AssemblyContext): number {
  let total = 0;
  for (const term of terms) {
    let value = 0;
    if (term.mm != null) {
      value = term.mm;
    } else if (term.standard) {
      value = context.standard[term.standard] ?? 0;
    } else if (term.thickness) {
      value = context.thickness(term.thickness);
    }
    total += value * (term.factor ?? 1);
  }
  return total;
}

/**
 * `count` copies of a repeated Part, spaced evenly in its span on its
 * thickness axis; dividers inside the span across x split it into sections.
 */
function copies(part: AssemblyPart, span: Box, count: number, thickness: number, dividers: AssembledPart[]): AssembledPart[] {
  const sections: [number, number][] = [];
  let start = span.x0;
  for (const divider of [...dividers].sort((first, second) => first.box.x0 - second.box.x0)) {
    const gap = part.repeat?.dividerGapMm ?? 0;
    if (divider.box.x0 > start + TOLERANCE_MM && divider.box.x1 < span.x1 - TOLERANCE_MM) {
      sections.push([start, divider.box.x0 - gap]);
      start = divider.box.x1 + gap;
    }
  }
  sections.push([start, span.x1]);

  const axis = part.thickness!;
  const result: AssembledPart[] = [];
  sections.forEach(([low, high], section) => {
    const area = withAxis(span, "X", low, high);
    const prefix = `${part.id}#${sections.length > 1 ? `${section + 1}.` : ""}`;
    const gap = (boxSize(area, axis) - count * thickness) / (count + 1);
    for (let index = 0; index < count; index++) {
      const copyLow = boxMin(area, axis) + gap * (index + 1) + thickness * index;
      result.push(assembled(part, prefix + (index + 1), withAxis(area, axis, copyLow, copyLow + thickness)));
    }
  });
  return result;
}

function withAxis(box: Box, axis: Axis, low: number, high: number): Box {
  return axis === "X" ? { ...box, x0: low, x1: high } : axis === "Y" ? { ...box, y0: low, y1: high } : { ...box, z0: low, z1: high };
}

function boxOf(extents: Map<Axis, [number, number]>): Box {
  const [x0, x1] = extents.get("X")!;
  const [y0, y1] = extents.get("Y")!;
  const [z0, z1] = extents.get("Z")!;
  return { x0, x1, y0, y1, z0, z1 };
}

function assembled(part: AssemblyPart, id: string, box: Box): AssembledPart {
  const kind = part.kind ?? "PANEL";
  const thickness = part.thickness ?? null;
  return {
    id, source: part.id, role: part.role, name: part.name ?? null, kind, front: !!part.front, material: part.material, box,
    thickness, grain: grainAxis(part.grain ?? null, thickness), banding: banding(part.banding ?? null, kind, thickness),
  };
}

/** Grain runs up unless the Part lies flat, then along x. */
export function grainAxis(grain: Axis | null, thickness: Axis | null): Axis {
  if (grain && grain !== thickness) {
    return grain;
  }
  return thickness === "Y" ? "X" : "Y";
}

/** A panel's in-plane axis that is not its grain. */
export function otherAxis(thickness: Axis, grain: Axis): Axis {
  return AXES.find(axis => axis !== thickness && axis !== grain)!;
}

/** A panel's front edge gets the carcass banding unless the template says otherwise. */
function banding(given: Banding | null, kind: AssemblyPartKind, thickness: Axis | null): Banding | null {
  if (given || kind !== "PANEL" || thickness === "Z") {
    return given;
  }
  return { faces: ["FRONT"], material: { role: "CARCASS_BANDING" } };
}

/** A front stack's Fronts, sized like a starter template's front rows; null until its area can be placed. */
function fronts(stack: FrontStack, extents: Extents, context: AssemblyContext): AssembledPart[] | null {
  const faces = stack.faces ?? {};
  const positions = new Map<Face, number>();
  for (const face of ["LEFT", "RIGHT", "BOTTOM", "TOP", "BACK"] as Face[]) {
    const attachment = faces[face];
    const value = attachment ? position(attachment, extents, context) : null;
    if (value === null) {
      return null;
    }
    positions.set(face, value);
  }

  const gap = context.standard.DOOR_GAP ?? 0;
  const left = positions.get("LEFT")!;
  const width = positions.get("RIGHT")! - left;
  const bottom = positions.get("BOTTOM")!;
  const back = positions.get("BACK")!;
  const thickness = context.thickness(stack.material);
  const frontBanding: Banding = { faces: ["LEFT", "RIGHT", "BOTTOM", "TOP"], material: { role: "FRONT_BANDING" } };

  const result: AssembledPart[] = [];
  sizeRows(stack.rows ?? [], positions.get("TOP")! - bottom, gap).forEach((row, index) => {
    const y0 = bottom + row.bottomMm;
    const y1 = y0 + row.heightMm;
    const id = `${stack.id}.${index + 1}`;
    if (row.kind === "DOOR") {
      const leaves = stack.splitAboveMm != null && width > stack.splitAboveMm ? 2 : 1;
      const leafWidth = (width - gap * leaves) / leaves;
      for (let leaf = 0; leaf < leaves; leaf++) {
        const x0 = left + gap / 2 + leaf * (leafWidth + gap);
        result.push({
          id: `${id}.${leaf + 1}`, source: stack.id, role: "DOOR", name: null, kind: "PANEL", front: true, material: stack.material,
          box: { x0, x1: x0 + leafWidth, y0, y1, z0: back, z1: back + thickness }, thickness: "Z", grain: "Y", banding: frontBanding,
        });
      }
    } else if (row.kind === "DRAWER" || row.kind === "FALSE_FRONT") {
      result.push({
        id, source: stack.id, role: row.kind === "DRAWER" ? "DRAWER_FRONT" : "FALSE_FRONT", name: null, kind: "PANEL", front: true,
        material: stack.material, box: { x0: left + gap / 2, x1: left + width - gap / 2, y0, y1, z0: back, z1: back + thickness },
        thickness: "Z", grain: "X", banding: frontBanding,
      });
    }
  });
  return result;
}

/** Each hardware item counted on its hosts, totalled by material in order of first use. */
function hardware(assembly: TemplateAssembly, parts: AssembledPart[], context: AssemblyContext): AssembledHardware[] {
  const totals = new Map<string, AssembledHardware>();
  for (const item of assembly.hardware ?? []) {
    const quantity = item.quantity;
    let total: number;
    if (!item.on) {
      total = count(quantity, { x0: 0, x1: context.width, y0: 0, y1: context.height, z0: 0, z1: context.depth });
    } else {
      total = parts
        .filter(part => part.source === item.on && (!item.frontKind || part.role === frontRole(item.frontKind)))
        .reduce((sum, host) => sum + count(quantity, host.box), 0);
    }
    if (total > 0) {
      const key = `${item.material.role ?? ""}|${item.material.materialId ?? ""}`;
      const current = totals.get(key);
      totals.set(key, { material: item.material, quantity: (current?.quantity ?? 0) + total });
    }
  }
  return [...totals.values()];
}

/** How many of an item one host takes. */
function count(quantity: Hardware["quantity"], host: Box): number {
  if (quantity.fixed != null) {
    return quantity.fixed;
  }
  const size = boxSize(host, quantity.axis ?? "Y");
  if (quantity.steps) {
    return quantity.steps.find(step => step.upToMm == null || size <= step.upToMm + TOLERANCE_MM)?.quantity ?? 0;
  }
  const byLength = Math.ceil(size / quantity.perMm! - TOLERANCE_MM);
  return Math.max(byLength, quantity.minimum ?? 0);
}

/** A Part flipped left for right within the Cabinet's width. */
function mirror(part: AssembledPart, width: number): AssembledPart {
  return { ...part, box: { ...part.box, x0: width - part.box.x1, x1: width - part.box.x0 } };
}

function frontRole(kind: FrontKind): PartRole | null {
  return kind === "DOOR" ? "DOOR" : kind === "DRAWER" ? "DRAWER_FRONT" : kind === "FALSE_FRONT" ? "FALSE_FRONT" : null;
}

/** The catalog material a template's material stands for in this Shop. */
export function materialIdOf(material: MaterialRef, standard: ConstructionStandard): string | null {
  if (material.materialId) {
    return material.materialId;
  }
  const byRole: Record<MaterialRole, string> = {
    CARCASS: standard.carcassMaterialId,
    BACK: standard.backMaterialId,
    FRONT: standard.frontMaterialId,
    CARCASS_BANDING: standard.carcassEdgeBandingId,
    FRONT_BANDING: standard.frontEdgeBandingId,
    CROWN_MOULDING: standard.crownMouldingId,
    HINGE: standard.hingeId,
    SHELF_PIN: standard.shelfPinId,
    LEG: standard.legId,
    PULL: standard.pullId,
    DRAWER_SLIDE: standard.drawerSlideId,
    DRAWER_BOX: standard.drawerBoxId,
  };
  return material.role ? byRole[material.role] ?? null : null;
}

/**
 * What a placed Cabinet brings to its template's assembly in this Shop, like
 * CabinetAssemblies.java; a blind corner with its blind side on the left is its
 * template mirrored.
 */
export function cabinetContext(cabinet: Cabinet, template: CabinetTemplate, standard: ConstructionStandard, materials: Material[]): AssemblyContext {
  const thicknesses = new Map(materials.map(material => [material.id, material.thicknessMm ?? 0]));
  return {
    width: cabinet.widthMm,
    height: cabinet.heightMm,
    depth: cabinet.depthMm,
    shelfCount: cabinet.shelfCount,
    dividers: cabinet.dividerPositionsMm ?? [],
    standard: { DOOR_GAP: standard.doorGapMm, TOE_KICK_HEIGHT: standard.toeKickHeightMm },
    thickness: material => thicknesses.get(materialIdOf(material, standard) ?? "") ?? 0,
    mirrored: (template.blindWidthMm ?? 0) > 0 && cabinet.blindSide === "LEFT",
  };
}
