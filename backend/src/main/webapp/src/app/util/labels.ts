import { MaterialKind } from "../catalog/model/material";
import { PartRole } from "../cutlist/model/project-output";
import { QuoteStatus } from "../quote/model/quote";
import { LabourMethod } from "../shop/model/shop";

export const PART_ROLE_LABELS: Record<PartRole, string> = {
  SIDE: $localize`:@@partRole.side:Côté`,
  BOTTOM: $localize`:@@partRole.bottom:Fond`,
  TOP: $localize`:@@partRole.top:Dessus`,
  STRETCHER: $localize`:@@partRole.stretcher:Traverse`,
  BACK: $localize`:@@partRole.back:Dos`,
  SHELF: $localize`:@@partRole.shelf:Tablette`,
  DOOR: $localize`:@@partRole.door:Porte`,
  DRAWER_FRONT: $localize`:@@partRole.drawerFront:Façade de tiroir`,
};

export const TEMPLATE_LABELS: Record<string, string> = {
  BASE_DOOR: $localize`:@@template.baseDoor:Bas, portes`,
  BASE_SINK: $localize`:@@template.baseSink:Bas d'évier`,
  BASE_DRAWERS: $localize`:@@template.baseDrawers:Bas, tiroirs`,
  WALL_DOOR: $localize`:@@template.wallDoor:Haut, portes`,
};

export const VIOLATION_LABELS: Record<string, string> = {
  INVALID_WALL: $localize`:@@violation.invalidWall:Le mur doit avoir une longueur et une hauteur.`,
  UNKNOWN_WALL: $localize`:@@violation.unknownWall:Le caisson est sur un mur qui n'existe plus.`,
  UNKNOWN_TEMPLATE: $localize`:@@violation.unknownTemplate:Le modèle du caisson est inconnu.`,
  WIDTH_OUT_OF_RANGE: $localize`:@@violation.width:La largeur est hors des limites du modèle.`,
  HEIGHT_OUT_OF_RANGE: $localize`:@@violation.height:La hauteur est hors des limites du modèle.`,
  DEPTH_OUT_OF_RANGE: $localize`:@@violation.depth:La profondeur est hors des limites du modèle.`,
  SHELF_COUNT_OUT_OF_RANGE: $localize`:@@violation.shelves:Le nombre de tablettes est hors des limites du modèle.`,
  BASE_NOT_ON_FLOOR: $localize`:@@violation.baseNotOnFloor:Un caisson du bas doit reposer au sol.`,
  BELOW_FLOOR: $localize`:@@violation.belowFloor:Le caisson descend sous le plancher.`,
  OUTSIDE_WALL: $localize`:@@violation.outsideWall:Le caisson dépasse du mur.`,
  ABOVE_WALL: $localize`:@@violation.aboveWall:Le caisson dépasse le plafond.`,
  OVERLAP: $localize`:@@violation.overlap:Le caisson chevauche un autre caisson.`,
};

export const QUOTE_STATUS_LABELS: Record<QuoteStatus, string> = {
  DRAFT: $localize`:@@quoteStatus.draft:Brouillon`,
  SENT: $localize`:@@quoteStatus.sent:Envoyée`,
  ACCEPTED: $localize`:@@quoteStatus.accepted:Acceptée`,
  DECLINED: $localize`:@@quoteStatus.declined:Refusée`,
};

export const LABOUR_METHOD_LABELS: Record<LabourMethod, string> = {
  OPERATION: $localize`:@@labourMethod.operation:Par opération`,
  LINEAR_FOOT: $localize`:@@labourMethod.linearFoot:Au pied linéaire`,
  HOURLY: $localize`:@@labourMethod.hourly:À l'heure`,
};

export const MATERIAL_KIND_LABELS: Record<MaterialKind, string> = {
  SHEET: $localize`:@@materialKind.sheet:Panneau`,
  EDGE_BANDING: $localize`:@@materialKind.edgeBanding:Bande de chant`,
  HARDWARE: $localize`:@@materialKind.hardware:Quincaillerie`,
};
