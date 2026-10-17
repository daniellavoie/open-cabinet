import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { APPLIANCE_KIND_LABELS, OPENING_KIND_LABELS, TEMPLATE_LABELS, VIOLATION_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { addAppliance, addCabinet, addOpening, addWall, ItemChanges, removeCabinet, removeItem, removeWall, updateCabinet, updateItem, updateWall } from "../design-edits";
import { Appliance, ApplianceKind, Cabinet, CabinetTemplate, Design, Opening, OpeningKind, Violation, Wall } from "../model/design";

const OPENING_KINDS: OpeningKind[] = ["WINDOW", "DOORWAY"];
const APPLIANCE_KINDS: ApplianceKind[] = ["FRIDGE", "RANGE", "DISHWASHER", "HOOD"];

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
  readonly openingKinds = OPENING_KINDS;
  readonly applianceKinds = APPLIANCE_KINDS;
  readonly itemLabels: Record<string, string> = { ...OPENING_KIND_LABELS, ...APPLIANCE_KIND_LABELS };

  @Input() design!: Design;
  @Input() templates: CabinetTemplate[] = [];
  @Input() units: Units = "IMPERIAL";
  /**
   * The selected Cabinet, Opening or Appliance.
   */
  @Input() selectedId: string | null = null;
  @Input() violations: Violation[] = [];
  @Input() cabinetLabels = new Map<string, string>();
  @Input() readOnly = false;

  @Output() designChange = new EventEmitter<Design>();
  @Output() selectItem = new EventEmitter<string | null>();

  newTemplateCode = "BASE_DOOR";
  newWallId = "";
  newItemKind: OpeningKind | ApplianceKind = "WINDOW";
  newItemWallId = "";

  ngOnChanges() {
    const firstWallId = this.design.walls[0]?.id ?? "";
    if (!this.design.walls.some(wall => wall.id === this.newWallId)) {
      this.newWallId = firstWallId;
    }
    if (!this.design.walls.some(wall => wall.id === this.newItemWallId)) {
      this.newItemWallId = firstWallId;
    }
  }

  /**
   * Openings, then Appliances, each along the Room.
   */
  get items(): (Opening | Appliance)[] {
    const byPlace = (first: Opening | Appliance, second: Opening | Appliance) =>
      this.wallNumber(first.wallId) - this.wallNumber(second.wallId) || first.offsetMm - second.offsetMm;
    return [...[...this.design.openings ?? []].sort(byPlace), ...[...this.design.appliances ?? []].sort(byPlace)];
  }

  get selectedItem(): Opening | Appliance | undefined {
    return this.items.find(item => item.id === this.selectedId);
  }

  get selectedAppliance(): Appliance | undefined {
    return this.design.appliances?.find(item => item.id === this.selectedId);
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
    return this.templates.find(template => template.code === this.selected?.templateCode);
  }

  wallNumber(wallId: string): number {
    return this.design.walls.findIndex(wall => wall.id === wallId) + 1;
  }

  violationsOf(id: string): Violation[] {
    return this.violations.filter(violation => violation.cabinetId === id || violation.itemId === id);
  }

  itemLabel(itemId: string): string {
    const item = this.items.find(candidate => candidate.id === itemId);
    return item ? this.itemLabels[item.kind] : "";
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
    const template = this.templates.find(item => item.code === this.newTemplateCode);
    if (!template || !this.newWallId) {
      return;
    }
    const result = addCabinet(this.design, template, this.newWallId, this.templates);
    this.designChange.emit(result.design);
    this.selectItem.emit(result.cabinetId);
  }

  updateSelected(changes: Partial<Cabinet>) {
    this.designChange.emit(updateCabinet(this.design, this.selectedId!, changes));
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
    const result = (OPENING_KINDS as string[]).includes(kind)
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
