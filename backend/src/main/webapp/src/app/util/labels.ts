import type { AnswerError, Remark } from "../assistant/model/conversation";
import { MaterialKind } from "../catalog/model/material";
import { Role } from "../member/model/member";
import { PartRole } from "../cutlist/model/project-output";
import { ApplianceKind, Axis, CabinetType, Face, FrontKind, MaterialRole, OpeningKind, StandardValue } from "../project/model/design";
import { NestingLayout } from "../project/model/project";
import type { TemplateRuleCode, TemplateSize } from "../project/template-rules";
import { QuoteStatus } from "../quote/model/quote";
import { LabourMethod } from "../shop/model/shop";
import { formatLength, Units } from "./units/units";

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
  OTHER: $localize`:@@partRole.other:Autre`,
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

export const CABINET_TYPE_LABELS: Record<CabinetType, string> = {
  BASE: $localize`:@@cabinetType.base:Bas`,
  WALL: $localize`:@@cabinetType.wall:Haut`,
  TALL: $localize`:@@cabinetType.tall:Colonne`,
};

export const FACE_LABELS: Record<Face, string> = {
  LEFT: $localize`:@@face.left:Gauche`,
  RIGHT: $localize`:@@face.right:Droite`,
  BOTTOM: $localize`:@@face.bottom:Dessous`,
  TOP: $localize`:@@face.top:Dessus`,
  BACK: $localize`:@@face.back:Arrière`,
  FRONT: $localize`:@@face.front:Avant`,
};

/** How a panel stands, by the axis its thickness runs along. */
export const THICKNESS_LABELS: Record<Axis, string> = {
  X: $localize`:@@thickness.x:Debout, de côté`,
  Y: $localize`:@@thickness.y:À plat`,
  Z: $localize`:@@thickness.z:Debout, de face`,
};

/** Which way a panel's grain runs. */
export const GRAIN_LABELS: Record<Axis, string> = {
  X: $localize`:@@grain.x:En largeur`,
  Y: $localize`:@@grain.y:En hauteur`,
  Z: $localize`:@@grain.z:En profondeur`,
};

export const AXIS_LABELS: Record<Axis, string> = {
  X: $localize`:@@axis.x:Largeur`,
  Y: $localize`:@@axis.y:Hauteur`,
  Z: $localize`:@@axis.z:Profondeur`,
};

export const MATERIAL_ROLE_LABELS: Record<MaterialRole, string> = {
  CARCASS: $localize`:@@materialRole.carcass:Panneau du caisson`,
  BACK: $localize`:@@materialRole.back:Panneau du dos`,
  FRONT: $localize`:@@materialRole.front:Panneau des façades`,
  CARCASS_BANDING: $localize`:@@materialRole.carcassBanding:Bande de chant du caisson`,
  FRONT_BANDING: $localize`:@@materialRole.frontBanding:Bande de chant des façades`,
  CROWN_MOULDING: $localize`:@@materialRole.crownMoulding:Moulure couronne`,
  HINGE: $localize`:@@materialRole.hinge:Charnière`,
  SHELF_PIN: $localize`:@@materialRole.shelfPin:Taquet`,
  LEG: $localize`:@@materialRole.leg:Patte`,
  PULL: $localize`:@@materialRole.pull:Poignée`,
  DRAWER_SLIDE: $localize`:@@materialRole.drawerSlide:Coulisse`,
  DRAWER_BOX: $localize`:@@materialRole.drawerBox:Boîte de tiroir`,
};

export const STANDARD_VALUE_LABELS: Record<StandardValue, string> = {
  DOOR_GAP: $localize`:@@standardValue.doorGap:Jeu des façades`,
  TOE_KICK_HEIGHT: $localize`:@@standardValue.toeKick:Hauteur du coup de pied`,
};

export const FRONT_KIND_LABELS: Record<FrontKind, string> = {
  DOOR: $localize`:@@frontKind.door:Porte`,
  DRAWER: $localize`:@@frontKind.drawer:Tiroir`,
  FALSE_FRONT: $localize`:@@frontKind.falseFront:Fausse façade`,
  OPENING: $localize`:@@frontKind.opening:Ouverture`,
};

export const TEMPLATE_SIZE_LABELS: Record<TemplateSize, string> = {
  MINIMUM: $localize`:@@templateSize.minimum:Minimum`,
  DEFAULT: $localize`:@@templateSize.default:Par défaut`,
  MAXIMUM: $localize`:@@templateSize.maximum:Maximum`,
};

export const TEMPLATE_RULE_LABELS: Record<TemplateRuleCode, string> = {
  DUPLICATE_NAME: $localize`:@@templateRule.duplicateName:Deux pièces portent le même nom.`,
  UNKNOWN_TARGET: $localize`:@@templateRule.unknownTarget:Une face est fixée à une pièce qui n'existe pas.`,
  WRONG_ATTACHMENTS: $localize`:@@templateRule.wrongAttachments:Il faut, sur chaque axe, deux de : la face basse, la face haute, la taille.`,
  UNRESOLVED: $localize`:@@templateRule.unresolved:La pièce ne peut pas être placée : ses fixations tournent en rond.`,
  NON_POSITIVE_SIZE: $localize`:@@templateRule.nonPositiveSize:La pièce n'a plus de taille.`,
  PARTS_OVERLAP: $localize`:@@templateRule.partsOverlap:La pièce chevauche une autre pièce.`,
  TOO_BIG_FOR_SHEET: $localize`:@@templateRule.tooBig:La pièce est plus grande que son panneau.`,
  NOT_TOUCHING: $localize`:@@templateRule.notTouching:La pièce ne touche aucune autre pièce.`,
  NO_PARTS: $localize`:@@templateRule.noParts:Le modèle n'a encore aucune pièce.`,
};

/** What each Design Assistant tool does, for its progress lines. */
export const ASSISTANT_TOOL_LABELS: Record<string, string> = {
  add_wall: $localize`:@@assistantTool.addWall:Ajouter un mur`,
  update_wall: $localize`:@@assistantTool.updateWall:Modifier un mur`,
  remove_wall: $localize`:@@assistantTool.removeWall:Retirer un mur`,
  add_opening: $localize`:@@assistantTool.addOpening:Ajouter une ouverture`,
  add_appliance: $localize`:@@assistantTool.addAppliance:Ajouter un électroménager`,
  add_filler: $localize`:@@assistantTool.addFiller:Ajouter un remplissage`,
  update_item: $localize`:@@assistantTool.updateItem:Modifier un élément`,
  remove_item: $localize`:@@assistantTool.removeItem:Retirer un élément`,
  add_cabinet: $localize`:@@assistantTool.addCabinet:Ajouter un caisson`,
  update_cabinet: $localize`:@@assistantTool.updateCabinet:Modifier un caisson`,
  add_divider: $localize`:@@assistantTool.addDivider:Ajouter un séparateur`,
  move_divider: $localize`:@@assistantTool.moveDivider:Déplacer un séparateur`,
  remove_divider: $localize`:@@assistantTool.removeDivider:Retirer un séparateur`,
  remove_cabinet: $localize`:@@assistantTool.removeCabinet:Retirer un caisson`,
  note_proposal: $localize`:@@assistantTool.noteProposal:Noter le résumé, les questions et les hypothèses`,
  view_design: $localize`:@@assistantTool.viewDesign:Relire le plan`,
};

export const ANSWER_ERROR_LABELS: Record<AnswerError, string> = {
  OFF: $localize`:@@answerError.off:L'assistant a été désactivé pour l'atelier.`,
  BUDGET_SPENT: $localize`:@@answerError.budgetSpent:Le budget du mois est dépensé : l'assistant reprendra le 1er du mois prochain.`,
  MODEL_ERROR: $localize`:@@answerError.modelError:L'assistant n'a pas pu répondre. Réessayez dans un moment.`,
  TOO_MANY_STEPS: $localize`:@@answerError.tooManySteps:L'assistant s'est arrêté : la demande prenait trop d'étapes. Précisez-la ou découpez-la.`,
  INTERRUPTED: $localize`:@@answerError.interrupted:L'assistant a été interrompu par un redémarrage. Renvoyez votre message.`,
};

/**
 * A Remark as a Member reads it: the Wall's number, what is off and by how
 * much, in the Shop's units. The Cabinets' ids stay with the assistant.
 */
export function remarkLabel(remark: Remark, wallNumber: number | null, units: Units): string {
  const measure = formatLength(remark.measureMm === null ? null : Math.abs(remark.measureMm), units);
  const limit = formatLength(remark.limitMm, units);
  const wall = wallNumber ?? "";
  switch (remark.code) {
    case "GAP": return $localize`:@@remark.gap:Mur ${wall}:wall: : espace de ${measure}:measure: dans la rangée de caissons.`;
    case "GAP_AT_END": return $localize`:@@remark.gapAtEnd:Mur ${wall}:wall: : bande de ${measure}:measure: au bout de la rangée.`;
    case "FILLER_WIDTH": return $localize`:@@remark.fillerWidth:Mur ${wall}:wall: : remplissage de ${measure}:measure:, hors de la largeur d'un remplissage.`;
    case "FILLER_MID_RUN": return $localize`:@@remark.fillerMidRun:Mur ${wall}:wall: : remplissage au milieu d'une rangée plutôt qu'à son bout.`;
    case "WIDTH_STEP": return $localize`:@@remark.widthStep:Mur ${wall}:wall: : caisson de ${measure}:measure: de large, hors des pas de ${limit}:limit:.`;
    case "CORNER_NOT_BLIND": return $localize`:@@remark.cornerNotBlind:Mur ${wall}:wall: : les rangées se rejoignent dans le coin sans caisson de coin aveugle.`;
    case "CORNER_CLEARANCE": return $localize`:@@remark.cornerClearance:Mur ${wall}:wall: : ${measure}:measure: seulement entre la rangée et le caisson de coin (au moins ${limit}:limit:).`;
    case "UPPER_CORNER_CLEARANCE": return $localize`:@@remark.upperCornerClearance:Mur ${wall}:wall: : caissons hauts serrés dans le coin (${measure}:measure:, au moins ${limit}:limit:).`;
    case "SINK_NOT_CENTRED": return $localize`:@@remark.sinkNotCentred:Mur ${wall}:wall: : l'évier est décentré de ${measure}:measure: sous la fenêtre.`;
    case "DISHWASHER_NOT_BESIDE_SINK": return $localize`:@@remark.dishwasherNotBesideSink:Mur ${wall}:wall: : le lave-vaisselle n'est pas à côté de l'évier.`;
    case "DISHWASHER_IN_CORNER": return $localize`:@@remark.dishwasherInCorner:Mur ${wall}:wall: : le lave-vaisselle est dans un coin.`;
    case "LANDING_RANGE": return $localize`:@@remark.landingRange:Mur ${wall}:wall: : ${measure}:measure: de comptoir d'un côté de la cuisinière (au moins ${limit}:limit:).`;
    case "LANDING_SINK": return $localize`:@@remark.landingSink:Mur ${wall}:wall: : ${measure}:measure: de comptoir d'un côté de l'évier (au moins ${limit}:limit:).`;
    case "LANDING_FRIDGE": return $localize`:@@remark.landingFridge:Mur ${wall}:wall: : ${measure}:measure: de comptoir à côté du réfrigérateur (au moins ${limit}:limit:).`;
    case "WORK_TRIANGLE": return $localize`:@@remark.workTriangle:Triangle de travail : ${measure}:measure: (limite ${limit}:limit:).`;
    case "WORK_TRIANGLE_BLOCKED": return $localize`:@@remark.workTriangleBlocked:Un caisson haut coupe le triangle de travail.`;
    case "UPPER_ALIGNMENT": return $localize`:@@remark.upperAlignment:Mur ${wall}:wall: : un caisson haut n'est pas aligné avec ce qui est dessous.`;
    case "UPPER_TOP_LINE": return $localize`:@@remark.upperTopLine:Mur ${wall}:wall: : un caisson haut sort de ${measure}:measure: de la ligne du haut.`;
    case "RANGE_CLEARANCE": return $localize`:@@remark.rangeClearance:Mur ${wall}:wall: : ${measure}:measure: seulement au-dessus de la cuisinière (au moins ${limit}:limit:).`;
    case "TALL_MID_RUN": return $localize`:@@remark.tallMidRun:Mur ${wall}:wall: : un caisson haut coupe le comptoir en deux.`;
    case "HOOD_WITHOUT_RANGE": return $localize`:@@remark.hoodWithoutRange:Mur ${wall}:wall: : une hotte sans cuisinière dessous.`;
    case "HOOD_OVER_RANGE": return $localize`:@@remark.hoodOverRange:Mur ${wall}:wall: : la hotte n'est pas centrée sur la cuisinière, ou est plus étroite.`;
    default: return remark.code;
  }
}
