import { TEMPLATE_LABELS } from "../util/labels";
import { checkConstructionRules } from "./construction-rules";
import { updateCabinet } from "./design-edits";
import { Cabinet, CabinetTemplate, Design, templateKey, Violation } from "./model/design";

/**
 * Template Versions (docs/adr/0005-cabinet-keeps-its-template-version.md):
 * a Cabinet keeps the version it was placed with until a Member updates or
 * reverts it.
 */

/**
 * A template's name: the app's for the starter library, the one typed for a
 * template a Member designed.
 */
export function templateName(templates: CabinetTemplate[], code: string): string {
  return TEMPLATE_LABELS[code] ?? templates.find(template => template.code === code && template.name)?.name ?? code;
}

/** Whether a template comes with the app rather than being designed by a Member. */
export function isStarter(code: string): boolean {
  return code in TEMPLATE_LABELS;
}

/**
 * The newest version of each template, in library order: what new Cabinets
 * are added from.
 */
export function latestVersions(templates: CabinetTemplate[]): CabinetTemplate[] {
  const latest = new Map<string, CabinetTemplate>();
  for (const template of templates) {
    const current = latest.get(template.code);
    if (!current || template.version > current.version) {
      latest.set(template.code, template);
    }
  }
  return [...latest.values()];
}

/**
 * Every published version of a template, oldest first.
 */
export function versionsOf(templates: CabinetTemplate[], code: string): CabinetTemplate[] {
  return templates
    .filter(template => template.code === code)
    .sort((first, second) => first.version - second.version);
}

/**
 * The Template Version a Cabinet uses.
 */
export function templateOf(templates: CabinetTemplate[], cabinet: Cabinet): CabinetTemplate | undefined {
  const key = templateKey(cabinet.templateCode, cabinet.templateVersion);
  return templates.find(template => templateKey(template.code, template.version) === key);
}

/**
 * Versions newer than the Cabinet's, oldest first: what updating it brings.
 */
export function newerVersions(templates: CabinetTemplate[], cabinet: Cabinet): CabinetTemplate[] {
  return versionsOf(templates, cabinet.templateCode).filter(template => template.version > cabinet.templateVersion);
}

/**
 * Cabinets on an older version than their template's newest.
 */
export function outdatedCabinets(design: Design, templates: CabinetTemplate[]): Cabinet[] {
  return design.cabinets.filter(cabinet => newerVersions(templates, cabinet).length > 0);
}

export interface VersionChange {
  cabinetId: string;
  version: number;
}

export interface RefusedChange {
  cabinetId: string;
  version: number;
  violations: Violation[];
}

/**
 * Moves Cabinets to other Template Versions, one at a time, keeping their own
 * values (size, shelves, dividers, Fronts, Trim). A change that would make its
 * Cabinet break a Construction Rule it did not break before is refused and
 * left out, so one Cabinet that no longer fits never holds back the others.
 */
export function changeVersions(design: Design, templates: CabinetTemplate[], changes: VersionChange[]): { design: Design; refused: RefusedChange[] } {
  let current = design;
  const refused: RefusedChange[] = [];
  for (const change of changes) {
    const brokenBefore = new Set(violationsOf(current, templates, change.cabinetId).map(violation => violation.code));
    const candidate = updateCabinet(current, change.cabinetId, { templateVersion: change.version });
    const broken = violationsOf(candidate, templates, change.cabinetId).filter(violation => !brokenBefore.has(violation.code));
    if (broken.length > 0) {
      refused.push({ ...change, violations: broken });
    } else {
      current = candidate;
    }
  }
  return { design: current, refused };
}

/**
 * Moves the given Cabinets to their template's newest version.
 */
export function updateToLatest(design: Design, templates: CabinetTemplate[], cabinetIds: string[]): { design: Design; refused: RefusedChange[] } {
  const latest = new Map(latestVersions(templates).map(template => [template.code, template.version]));
  const changes = design.cabinets
    .filter(cabinet => cabinetIds.includes(cabinet.id))
    .map(cabinet => ({ cabinetId: cabinet.id, version: latest.get(cabinet.templateCode) ?? cabinet.templateVersion }))
    .filter(change => change.version !== design.cabinets.find(cabinet => cabinet.id === change.cabinetId)!.templateVersion);
  return changeVersions(design, templates, changes);
}

function violationsOf(design: Design, templates: CabinetTemplate[], cabinetId: string): Violation[] {
  return checkConstructionRules(design, templates).filter(violation => violation.cabinetId === cabinetId);
}
