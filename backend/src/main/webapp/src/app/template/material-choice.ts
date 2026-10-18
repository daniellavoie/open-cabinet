import { Material, MaterialKind } from "../catalog/model/material";
import { MaterialRef, MaterialRole } from "../project/model/design";
import { MATERIAL_ROLE_LABELS } from "../util/labels";

/**
 * A template's material as a select chooses it: one of the Construction
 * Standard's roles, which follows the Shop's choices, or a catalog material.
 */
export interface MaterialChoice {
  value: string;
  label: string;
}

/** The roles and catalog materials that fit where a material is chosen. */
export interface MaterialChoices {
  roles: MaterialChoice[];
  catalog: MaterialChoice[];
}

export function choiceValue(material: MaterialRef | null | undefined): string {
  return material?.materialId ? `id:${material.materialId}` : material?.role ? `role:${material.role}` : "";
}

export function fromChoice(value: string): MaterialRef {
  return value.startsWith("id:") ? { materialId: value.slice(3) } : { role: value.slice(5) as MaterialRole };
}

export function choices(roles: MaterialRole[], kinds: MaterialKind[], materials: Material[]): MaterialChoices {
  return {
    roles: roles.map(role => ({ value: `role:${role}`, label: MATERIAL_ROLE_LABELS[role] })),
    catalog: materials.filter(material => kinds.includes(material.kind))
      .map(material => ({ value: `id:${material.id}`, label: material.name })),
  };
}

export const PANEL_ROLES: MaterialRole[] = ["CARCASS", "BACK", "FRONT"];
export const BANDING_ROLES: MaterialRole[] = ["CARCASS_BANDING", "FRONT_BANDING"];
export const MOULDING_ROLES: MaterialRole[] = ["CROWN_MOULDING"];
export const HARDWARE_ROLES: MaterialRole[] = ["HINGE", "PULL", "DRAWER_SLIDE", "DRAWER_BOX", "SHELF_PIN", "LEG"];
