import { CABINET, faceAxis, faceOf } from "../project/assembly";
import {
  AssemblyPart, Attachment, Axis, Face, FrontStack, Hardware, MaterialRole, StackRow, TemplateAssembly,
} from "../project/model/design";

/**
 * Edits to a template's assembly in the designer, each returning a new one.
 * Parts and front stacks share one set of names: Attachments, hardware and
 * the divider refer to them by it.
 */

export const FACES: Face[] = ["LEFT", "RIGHT", "BOTTOM", "TOP", "BACK", "FRONT"];

const STACK_FACES: Face[] = ["LEFT", "RIGHT", "BOTTOM", "TOP", "BACK"];

function cabinetFace(face: Face, ...offset: Attachment["offset"] & object): Attachment {
  return offset.length > 0 ? { to: CABINET, face, offset } : { to: CABINET, face };
}

/** Every name used by a Part, front stack or the divider. */
export function names(assembly: TemplateAssembly): string[] {
  return [...(assembly.parts ?? []).map(part => part.id), ...(assembly.frontStacks ?? []).map(stack => stack.id),
    ...(assembly.divider ? [assembly.divider.id] : [])];
}

/** `base`, else `base2`, `base3`… whichever is free. */
export function uniqueName(assembly: TemplateAssembly, base: string): string {
  const taken = new Set(names(assembly));
  if (!taken.has(base)) {
    return base;
  }
  let index = 2;
  while (taken.has(`${base}${index}`)) {
    index++;
  }
  return `${base}${index}`;
}

/**
 * A new panel of `material`, lying flat across the whole Cabinet at its
 * bottom: placed, so it can be moved by attaching its faces.
 */
export function addPanel(assembly: TemplateAssembly, material: MaterialRole, name: string): { assembly: TemplateAssembly; id: string } {
  const id = uniqueName(assembly, "panel");
  const part: AssemblyPart = {
    id, role: "OTHER", name, kind: "PANEL", material: { role: material }, thickness: "Y",
    faces: {
      LEFT: cabinetFace("LEFT"), RIGHT: cabinetFace("RIGHT"), BOTTOM: cabinetFace("BOTTOM"),
      BACK: cabinetFace("BACK"), FRONT: cabinetFace("FRONT"),
    },
  };
  return { assembly: { ...assembly, parts: [...assembly.parts ?? [], part] }, id };
}

/** A new moulding along the Cabinet's top front, 2" by 2". */
export function addMoulding(assembly: TemplateAssembly, name: string): { assembly: TemplateAssembly; id: string } {
  const id = uniqueName(assembly, "moulding");
  const part: AssemblyPart = {
    id, role: "OTHER", name, kind: "MOULDING", material: { role: "CROWN_MOULDING" },
    faces: { LEFT: cabinetFace("LEFT"), RIGHT: cabinetFace("RIGHT"), TOP: cabinetFace("TOP"), FRONT: cabinetFace("FRONT") },
    sizes: { Y: [{ mm: 50.8 }], Z: [{ mm: 50.8 }] },
  };
  return { assembly: { ...assembly, parts: [...assembly.parts ?? [], part] }, id };
}

/** A new front stack of one door over the whole front, behind the Cabinet's front face. */
export function addStack(assembly: TemplateAssembly): { assembly: TemplateAssembly; id: string } {
  const id = uniqueName(assembly, "fronts");
  const stack: FrontStack = {
    id, material: { role: "FRONT" }, splitAboveMm: 609.6, rows: [{ kind: "DOOR" }],
    faces: {
      LEFT: cabinetFace("LEFT"), RIGHT: cabinetFace("RIGHT"), BOTTOM: cabinetFace("BOTTOM"), TOP: cabinetFace("TOP"),
      BACK: cabinetFace("FRONT", { thickness: { role: "FRONT" }, factor: -1 }),
    },
  };
  return { assembly: { ...assembly, frontStacks: [...assembly.frontStacks ?? [], stack] }, id };
}

export function addHardware(assembly: TemplateAssembly): TemplateAssembly {
  const item: Hardware = { material: { role: "HINGE" }, quantity: { fixed: 1 } };
  return { ...assembly, hardware: [...assembly.hardware ?? [], item] };
}

export function updateHardware(assembly: TemplateAssembly, index: number, item: Hardware): TemplateAssembly {
  return { ...assembly, hardware: (assembly.hardware ?? []).map((current, at) => at === index ? item : current) };
}

export function removeHardware(assembly: TemplateAssembly, index: number): TemplateAssembly {
  return { ...assembly, hardware: (assembly.hardware ?? []).filter((_, at) => at !== index) };
}

/** Whether Cabinets of this template take dividers; a new divider spans the Cabinet's height and depth. */
export function setDivider(assembly: TemplateAssembly, enabled: boolean): TemplateAssembly {
  if (!enabled) {
    return { ...assembly, divider: null };
  }
  if (assembly.divider) {
    return assembly;
  }
  const divider: AssemblyPart = {
    id: uniqueName(assembly, "divider"), role: "DIVIDER", material: { role: "CARCASS" }, thickness: "X",
    faces: { BOTTOM: cabinetFace("BOTTOM"), TOP: cabinetFace("TOP"), BACK: cabinetFace("BACK"), FRONT: cabinetFace("FRONT") },
  };
  return { ...assembly, divider };
}

/** The Part, or the divider, named `id`. */
export function partNamed(assembly: TemplateAssembly, id: string): AssemblyPart | null {
  return (assembly.parts ?? []).find(part => part.id === id) ?? (assembly.divider?.id === id ? assembly.divider : null);
}

export function updatePart(assembly: TemplateAssembly, id: string, part: AssemblyPart): TemplateAssembly {
  if (assembly.divider?.id === id) {
    return { ...assembly, divider: part };
  }
  return { ...assembly, parts: (assembly.parts ?? []).map(current => current.id === id ? part : current) };
}

export function updateStack(assembly: TemplateAssembly, id: string, stack: FrontStack): TemplateAssembly {
  return { ...assembly, frontStacks: (assembly.frontStacks ?? []).map(current => current.id === id ? stack : current) };
}

/**
 * Removes a Part or front stack and the hardware counted on it. Attachments
 * to it stay, and show as broken until they are moved.
 */
export function remove(assembly: TemplateAssembly, id: string): TemplateAssembly {
  return {
    ...assembly,
    parts: (assembly.parts ?? []).filter(part => part.id !== id),
    frontStacks: (assembly.frontStacks ?? []).filter(stack => stack.id !== id),
    divider: assembly.divider?.id === id ? null : assembly.divider,
    hardware: (assembly.hardware ?? []).filter(item => item.on !== id),
  };
}

/**
 * Renames a Part or front stack and everything that refers to it. Refused
 * (the assembly is returned unchanged) when the new name is empty or taken.
 */
export function rename(assembly: TemplateAssembly, from: string, to: string): TemplateAssembly {
  const name = to.trim();
  if (!name || name === CABINET || names(assembly).includes(name)) {
    return assembly;
  }
  const faces = <T extends Partial<Record<Face, Attachment>>>(all: T | null | undefined): T | null | undefined =>
    all && Object.fromEntries(Object.entries(all).map(([face, attachment]) =>
      [face, attachment.to === from ? { ...attachment, to: name } : attachment])) as T;
  const part = (current: AssemblyPart): AssemblyPart =>
    ({ ...current, id: current.id === from ? name : current.id, faces: faces(current.faces) });
  return {
    ...assembly,
    parts: (assembly.parts ?? []).map(part),
    divider: assembly.divider ? part(assembly.divider) : assembly.divider,
    frontStacks: (assembly.frontStacks ?? []).map(stack =>
      ({ ...stack, id: stack.id === from ? name : stack.id, faces: faces(stack.faces) })),
    hardware: (assembly.hardware ?? []).map(item => item.on === from ? { ...item, on: name } : item),
  };
}

/**
 * Holds `face` of a Part against `target`, keeping its offset. A Part needs
 * two of its low face, high face and size on each axis, so the attachment
 * makes room: on a panel's thickness axis it replaces the opposite face, and
 * elsewhere it drops a size that would be one too many.
 */
export function attach(part: AssemblyPart, face: Face, target: Attachment | null): AssemblyPart {
  const faces = { ...part.faces ?? {} };
  const sizes = { ...part.sizes ?? {} };
  const axis = faceAxis(face);
  if (!target) {
    delete faces[face];
    return { ...part, faces, sizes };
  }
  faces[face] = { ...target, offset: faces[face]?.offset ?? target.offset };
  const opposite = faceOf(axis, face === faceOf(axis, false));
  if (axis === part.thickness && !part.repeat) {
    delete faces[opposite];
  } else if (faces[opposite] && sizes[axis]) {
    delete sizes[axis];
  }
  return { ...part, faces, sizes };
}

/** A front stack's face held against `target`; a stack needs all five of its faces. */
export function attachStack(stack: FrontStack, face: Face, target: Attachment): FrontStack {
  if (!STACK_FACES.includes(face)) {
    return stack;
  }
  const faces = { ...stack.faces ?? {} };
  faces[face] = { ...target, offset: faces[face]?.offset ?? target.offset };
  return { ...stack, faces };
}

export function setRows(stack: FrontStack, rows: StackRow[]): FrontStack {
  return { ...stack, rows };
}

/** The axis a face lies across, for labels and checks. */
export function axisOf(face: Face): Axis {
  return faceAxis(face);
}

export { STACK_FACES };
