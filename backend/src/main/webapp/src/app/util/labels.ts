import { MaterialKind } from "../catalog/model/material";
import { Role } from "../member/model/member";
import { PartRole } from "../cutlist/model/project-output";
import { ApplianceKind, OpeningKind } from "../project/model/design";
import { NestingLayout } from "../project/model/project";
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
  FALSE_FRONT: $localize`:@@partRole.falseFront:Fausse façade`,
  BLIND_PANEL: $localize`:@@partRole.blindPanel:Panneau d'angle aveugle`,
  DIVIDER: $localize`:@@partRole.divider:Séparateur`,
  FINISHED_END: $localize`:@@partRole.finishedEnd:Panneau de finition`,
  FILLER: $localize`:@@partRole.filler:Remplissage`,
  TOE_KICK: $localize`:@@partRole.toeKick:Plinthe`,
};

export const TEMPLATE_LABELS: Record<string, string> = {
  BASE_DOOR: $localize`:@@template.baseDoor:Bas, portes`,
  BASE_DOOR_DRAWER: $localize`:@@template.baseDoorDrawer:Bas, tiroir et portes`,
  BASE_BLIND_CORNER: $localize`:@@template.baseBlindCorner:Bas, coin aveugle`,
  TALL_PANTRY: $localize`:@@template.tallPantry:Colonne garde-manger`,
  TALL_OVEN: $localize`:@@template.tallOven:Colonne four`,
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
  OVERLAP: $localize`:@@violation.overlap:Chevauche un autre élément du même genre.`,
  ITEM_OUTSIDE_WALL: $localize`:@@violation.itemOutsideWall:Dépasse du mur.`,
  BLOCKS_OPENING: $localize`:@@violation.blocksOpening:Le caisson bloque une fenêtre ou une porte.`,
  OVERLAPS_APPLIANCE: $localize`:@@violation.overlapsAppliance:Le caisson chevauche un électroménager.`,
  CORNER_OVERLAP: $localize`:@@violation.cornerOverlap:Le caisson heurte un caisson ou un électroménager du mur voisin.`,
  DIVIDERS_NOT_ALLOWED: $localize`:@@violation.dividersNotAllowed:Les séparateurs ne vont que derrière des portes.`,
  SECTION_TOO_NARROW: $localize`:@@violation.sectionTooNarrow:Une section entre séparateurs fait moins de 6 po (152 mm).`,
  FRONT_BLOCKED: $localize`:@@violation.frontBlocked:Un élément du mur voisin bloque les portes ou tiroirs du caisson.`,
};

export const OPENING_KIND_LABELS: Record<OpeningKind, string> = {
  WINDOW: $localize`:@@openingKind.window:Fenêtre`,
  DOORWAY: $localize`:@@openingKind.doorway:Porte`,
};

export const FILLER_LABEL = $localize`:@@itemKind.filler:Remplissage`;

export const APPLIANCE_KIND_LABELS: Record<ApplianceKind, string> = {
  FRIDGE: $localize`:@@applianceKind.fridge:Réfrigérateur`,
  RANGE: $localize`:@@applianceKind.range:Cuisinière`,
  DISHWASHER: $localize`:@@applianceKind.dishwasher:Lave-vaisselle`,
  HOOD: $localize`:@@applianceKind.hood:Hotte`,
};

export const NESTING_LAYOUT_LABELS: Record<NestingLayout, string> = {
  RIP_FIRST: $localize`:@@nestingLayout.ripFirst:Refendre d'abord`,
  CROSSCUT_FIRST: $localize`:@@nestingLayout.crosscutFirst:Tronçonner d'abord`,
};

export const QUOTE_STATUS_LABELS: Record<QuoteStatus, string> = {
  DRAFT: $localize`:@@quoteStatus.draft:Brouillon`,
  SENT: $localize`:@@quoteStatus.sent:Envoyée`,
  ACCEPTED: $localize`:@@quoteStatus.accepted:Acceptée`,
  DECLINED: $localize`:@@quoteStatus.declined:Refusée`,
  SUPERSEDED: $localize`:@@quoteStatus.superseded:Remplacée`,
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
  FRONT: $localize`:@@materialKind.front:Façades commandées`,
  MOULDING: $localize`:@@materialKind.moulding:Moulures`,
};

export const ROLE_LABELS: Record<Role, string> = {
  OWNER: $localize`:@@role.owner:Propriétaire`,
  DESIGNER: $localize`:@@role.designer:Concepteur`,
};
