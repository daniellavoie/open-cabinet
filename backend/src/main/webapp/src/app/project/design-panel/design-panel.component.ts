import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { Material } from "../../catalog/model/material";
import { ShopCabinetTemplate } from "../../template/shop-template-service";
import { APPLIANCE_KIND_LABELS, FILLER_LABEL, OPENING_KIND_LABELS, TEMPLATE_LABELS, VIOLATION_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { addAppliance, addCabinet, addDivider, addFiller, addOpening, addWall, ItemChanges, moveDivider, removeCabinet, removeDivider, removeItem, removeWall, updateCabinet, updateItem, updateWall } from "../design-edits";
import { allowsDividers, Appliance, ApplianceKind, Cabinet, CabinetTemplate, Design, Filler, Opening, OpeningKind, Violation, Wall } from "../model/design";
import { changeVersions, latestVersions, newerVersions, RefusedChange, templateOf, versionsOf } from "../template-versions";

const OPENING_KINDS: OpeningKind[] = ["WINDOW", "DOORWAY"];
const APPLIANCE_KINDS: ApplianceKind[] = ["FRIDGE", "RANGE", "DISHWASHER", "HOOD"];

type ItemKind = OpeningKind | ApplianceKind | "FILLER";
type Item = Opening | Appliance | Filler;

/**
 * Walls, Openings, Appliances, Cabinets and the broken Construction Rules of
 * a design, edited as forms next to the 3D view.
 */
@Component({
  selector: "design-panel",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, LengthInputComponent, NgFor, NgIf],
  styleUrls: ["./design-panel.component.css"],
  templateUrl: "./design-panel.component.html",
})
export class DesignPanelComponent implements OnChanges {
  readonly templateLabels = TEMPLATE_LABELS;
  readonly violationLabels = VIOLATION_LABELS;
  readonly allowsDividers = allowsDividers;
  readonly openingKinds = OPENING_KINDS;
  readonly applianceKinds = APPLIANCE_KINDS;
  readonly itemLabels: Record<ItemKind, string> = { ...OPENING_KIND_LABELS, ...APPLIANCE_KIND_LABELS, FILLER: FILLER_LABEL };

  @Input() design!: Design;
  @Input() templates: CabinetTemplate[] = [];
  @Input() units: Units = "IMPERIAL";
  /**
   * The selected Cabinet, Opening or Appliance.
   */
  @Input() selectedId: string | null = null;
  @Input() violations: Violation[] = [];
  /** Cabinets on an older Template Version than their template's newest. */
  @Input() outdatedIds = new Set<string>();
  @Input() cabinetLabels = new Map<string, string>();
  @Input() readOnly = false;
  /** The Shop's ordered Front styles. */
  @Input() frontStyles: Material[] = [];
  @Input() shopTemplates: ShopCabinetTemplate[] = [];
  /** Owners may save a Cabinet as a Shop template. */
  @Input() canSaveTemplates = false;

  @Output() designChange = new EventEmitter<Design>();
  @Output() selectItem = new EventEmitter<string | null>();
  @Output() saveTemplate = new EventEmitter<{ name: string; cabinet: Cabinet }>();

  /** A starter template's code, or "shop:" and a Shop template's id. */
  newTemplateCode = "BASE_DOOR";
  templateName: string | null = null;
  newWallId = "";
  newItemKind: ItemKind = "WINDOW";
  newItemWallId = "";
  /** The selected Cabinet's last version change, refused for breaking Construction Rules. */
  refused: RefusedChange[] = [];

  ngOnChanges(changes: SimpleChanges) {
    if (changes["selectedId"]) {
      this.refused = [];
    }
    const firstWallId = this.design.walls[0]?.id ?? "";
    if (!this.design.walls.some(wall => wall.id === this.newWallId)) {
      this.newWallId = firstWallId;
    }
    if (!this.design.walls.some(wall => wall.id === this.newItemWallId)) {
      this.newItemWallId = firstWallId;
    }
  }

  /**
   * Openings, then Appliances, then Fillers, each along the Room.
   */
  get items(): Item[] {
    const byPlace = (first: Item, second: Item) =>
      this.wallNumber(first.wallId) - this.wallNumber(second.wallId) || first.offsetMm - second.offsetMm;
    return [
      ...[...this.design.openings ?? []].sort(byPlace),
      ...[...this.design.appliances ?? []].sort(byPlace),
      ...[...this.design.fillers ?? []].sort(byPlace),
    ];
  }

  get selectedItem(): Item | undefined {
    return this.items.find(item => item.id === this.selectedId);
  }

  /**
   * The selected Appliance or Filler, which have a depth.
   */
  get selectedDeep(): Appliance | Filler | undefined {
    return [...this.design.appliances ?? [], ...this.design.fillers ?? []].find(item => item.id === this.selectedId);
  }

  kindOf(item: Item): ItemKind {
    return "kind" in item ? item.kind : "FILLER";
  }

  /**
   * Cabinets in label order (C1, C2, …), which follows the Room.
   */
  get orderedCabinets(): Cabinet[] {
    const order = (cabinet: Cabinet) => Number(this.cabinetLabels.get(cabinet.id)?.slice(1) ?? Number.MAX_SAFE_INTEGER);
    return [...this.design.cabinets].sort((first, second) => order(first) - order(second));
  }

  get selected(): Cabinet | undefined {
    return this.design.cabinets.find(cabinet => cabinet.id === this.selectedId);
  }

  get selectedTemplate(): CabinetTemplate | undefined {
    return this.selected ? templateOf(this.templates, this.selected) : undefined;
  }

  /**
   * What new Cabinets are added from: each template's newest version.
   */
  get starterTemplates(): CabinetTemplate[] {
    return latestVersions(this.templates);
  }

  /** Every version of the selected Cabinet's template, oldest first. */
  get selectedVersions(): CabinetTemplate[] {
    return this.selected ? versionsOf(this.templates, this.selected.templateCode) : [];
  }

  /** Versions newer than the selected Cabinet's, with their change notes. */
  get selectedNewer(): CabinetTemplate[] {
    return this.selected ? newerVersions(this.templates, this.selected) : [];
  }

  wallNumber(wallId: string): number {
    return this.design.walls.findIndex(wall => wall.id === wallId) + 1;
  }

  violationsOf(id: string): Violation[] {
    return this.violations.filter(violation => violation.cabinetId === id || violation.itemId === id);
  }

  itemLabel(itemId: string): string {
    const item = this.items.find(candidate => candidate.id === itemId);
    return item ? this.itemLabels[this.kindOf(item)] : "";
  }

  addWall() {
    this.designChange.emit(addWall(this.design));
  }

  updateWall(wall: Wall, changes: Partial<Wall>) {
    this.designChange.emit(updateWall(this.design, wall.id, changes));
  }

  removeWall(wall: Wall) {
    this.designChange.emit(removeWall(this.design, wall.id));
  }

  addCabinet() {
    const shopTemplate = this.shopTemplates.find(item => `shop:${item.id}` === this.newTemplateCode);
    const code = shopTemplate?.baseCode ?? this.newTemplateCode;
    const template = this.starterTemplates.find(item => item.code === code);
    if (!template || !this.newWallId) {
      return;
    }
    const result = addCabinet(this.design, template, this.newWallId, this.templates, shopTemplate?.preset);
    this.designChange.emit(result.design);
    this.selectItem.emit(result.cabinetId);
  }

  updateSelected(changes: Partial<Cabinet>) {
    this.designChange.emit(updateCabinet(this.design, this.selectedId!, changes));
  }

  /**
   * Updates or reverts the selected Cabinet to another Template Version,
   * unless it would break a Construction Rule.
   */
  changeSelectedVersion(version: number) {
    const result = changeVersions(this.design, this.templates, [{ cabinetId: this.selectedId!, version }]);
    this.refused = result.refused;
    if (result.refused.length === 0) {
      this.designChange.emit(result.design);
    }
  }

  addDivider() {
    this.designChange.emit(addDivider(this.design, this.selectedId!));
  }

  moveDivider(index: number, positionMm: number) {
    this.designChange.emit(moveDivider(this.design, this.selectedId!, index, positionMm));
  }

  removeDivider(index: number) {
    this.designChange.emit(removeDivider(this.design, this.selectedId!, index));
  }

  saveSelectedAsTemplate() {
    const name = this.templateName?.trim();
    if (name && this.selected) {
      this.saveTemplate.emit({ name, cabinet: this.selected });
      this.templateName = null;
    }
  }

  removeSelected() {
    this.designChange.emit(removeCabinet(this.design, this.selectedId!));
    this.selectItem.emit(null);
  }

  addItem() {
    if (!this.newItemWallId) {
      return;
    }
    const kind = this.newItemKind;
    const result = kind === "FILLER"
      ? addFiller(this.design, this.newItemWallId)
      : (OPENING_KINDS as string[]).includes(kind)
        ? addOpening(this.design, kind as OpeningKind, this.newItemWallId)
        : addAppliance(this.design, kind as ApplianceKind, this.newItemWallId);
    this.designChange.emit(result.design);
    this.selectItem.emit(result.itemId);
  }

  updateSelectedItem(changes: ItemChanges) {
    this.designChange.emit(updateItem(this.design, this.selectedId!, changes));
  }

  removeSelectedItem() {
    this.designChange.emit(removeItem(this.design, this.selectedId!));
    this.selectItem.emit(null);
  }
}
