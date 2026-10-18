import { AssembledPart, assemble, boxMax, boxMin, boxSize, Box, otherAxis, TOLERANCE_MM } from "./assembly";
import { Axis, CabinetTemplate, MaterialRef, StandardValue } from "./model/design";

/**
 * The rules an assembled template must pass before it is published (ADR 0006),
 * at its minimum, default and maximum size with as many shelves as it allows,
 * as TemplateRules.java checks them on the server. Kept in step by
 * template-assembly/rules.json.
 */

export type TemplateSize = "MINIMUM" | "DEFAULT" | "MAXIMUM";

export type TemplateRuleCode = "DUPLICATE_NAME" | "UNKNOWN_TARGET" | "WRONG_ATTACHMENTS" | "UNRESOLVED" | "NON_POSITIVE_SIZE"
  | "PARTS_OVERLAP" | "TOO_BIG_FOR_SHEET" | "NOT_TOUCHING";

export interface TemplateViolation {
  code: TemplateRuleCode;
  partId: string | null;
  size: TemplateSize | null;
}

export interface SheetSize {
  lengthMm: number;
  widthMm: number;
  grain: boolean;
}

/** What the Shop brings: Standard values, thicknesses and, for sheet materials, their sheet (null otherwise). */
export interface TemplateShop {
  standard: Partial<Record<StandardValue, number>>;
  thickness: (material: MaterialRef) => number;
  sheet: (material: MaterialRef) => SheetSize | null;
  trimMm: number;
}

const AXES: Axis[] = ["X", "Y", "Z"];
const SIZES: TemplateSize[] = ["MINIMUM", "DEFAULT", "MAXIMUM"];

export function checkTemplate(template: CabinetTemplate, shop: TemplateShop): TemplateViolation[] {
  const assembly = template.assembly;
  if (!assembly) {
    return [];
  }
  const names = [...(assembly.parts ?? []).map(part => part.id), ...(assembly.frontStacks ?? []).map(stack => stack.id),
    ...(assembly.divider ? [assembly.divider.id] : [])];
  if (new Set(names).size !== names.length) {
    return [{ code: "DUPLICATE_NAME", partId: null, size: null }];
  }

  const violations: TemplateViolation[] = [];
  const seen = new Set<string>();
  for (const size of SIZES) {
    const pick = (minimum: number, fallback: number, maximum: number) =>
      size === "MINIMUM" ? minimum : size === "DEFAULT" ? fallback : maximum;
    const result = assemble(assembly, {
      width: pick(template.minWidthMm, template.defaultWidthMm, template.maxWidthMm),
      height: pick(template.minHeightMm, template.defaultHeightMm, template.maxHeightMm),
      depth: pick(template.minDepthMm, template.defaultDepthMm, template.maxDepthMm),
      shelfCount: template.maxShelfCount,
      dividers: [],
      standard: shop.standard,
      thickness: shop.thickness,
    });
    const found: TemplateViolation[] = [
      ...result.violations.map(violation => ({ code: violation.code, partId: violation.partId, size })),
      ...geometry(result.parts, shop, size),
    ];
    for (const violation of found) {
      // Each problem once, at the first size it shows.
      const key = `${violation.code}/${violation.partId}`;
      if (!seen.has(key)) {
        seen.add(key);
        violations.push(violation);
      }
    }
  }
  return violations;
}

function geometry(parts: AssembledPart[], shop: TemplateShop, size: TemplateSize): TemplateViolation[] {
  const violations: TemplateViolation[] = [];
  for (const part of parts) {
    if (AXES.some(axis => boxSize(part.box, axis) <= TOLERANCE_MM)) {
      violations.push({ code: "NON_POSITIVE_SIZE", partId: part.source, size });
    }
  }
  for (let second = 0; second < parts.length; second++) {
    for (let first = 0; first < second; first++) {
      if (overlap(parts[first].box, parts[second].box)) {
        violations.push({ code: "PARTS_OVERLAP", partId: parts[second].source, size });
      }
    }
  }
  for (const part of parts) {
    if (part.kind === "PANEL" && !fitsSheet(part, shop)) {
      violations.push({ code: "TOO_BIG_FOR_SHEET", partId: part.source, size });
    }
  }
  if (parts.length > 1) {
    for (const part of parts) {
      if (!parts.some(other => other !== part && touch(part.box, other.box))) {
        violations.push({ code: "NOT_TOUCHING", partId: part.source, size });
      }
    }
  }
  return violations;
}

export function overlap(first: Box, second: Box): boolean {
  return AXES.every(axis =>
    Math.min(boxMax(first, axis), boxMax(second, axis)) - Math.max(boxMin(first, axis), boxMin(second, axis)) > TOLERANCE_MM);
}

export function touch(first: Box, second: Box): boolean {
  return AXES.every(axis =>
    boxMin(first, axis) <= boxMax(second, axis) + TOLERANCE_MM && boxMin(second, axis) <= boxMax(first, axis) + TOLERANCE_MM);
}

/** Whether a panel can be cut from its sheet inside the trim; unknown sheets pass. */
function fitsSheet(part: AssembledPart, shop: TemplateShop): boolean {
  const sheet = shop.sheet(part.material);
  if (!sheet || !part.thickness) {
    return true;
  }
  const along = boxSize(part.box, part.grain);
  const across = boxSize(part.box, otherAxis(part.thickness, part.grain));
  const length = sheet.lengthMm - 2 * shop.trimMm;
  const width = sheet.widthMm - 2 * shop.trimMm;
  const fits = along <= length + TOLERANCE_MM && across <= width + TOLERANCE_MM;
  const turned = across <= length + TOLERANCE_MM && along <= width + TOLERANCE_MM;
  return sheet.grain ? fits : fits || turned;
}
