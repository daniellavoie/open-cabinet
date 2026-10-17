import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { TEMPLATE_LABELS, VIOLATION_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { addCabinet, addWall, removeCabinet, removeWall, updateCabinet, updateWall } from "../design-edits";
import { Cabinet, CabinetTemplate, Design, Violation, Wall } from "../model/design";

/**
 * Walls, Cabinets and the broken Construction Rules of a design, edited as
 * forms next to the 3D view.
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

  @Input() design!: Design;
  @Input() templates: CabinetTemplate[] = [];
  @Input() units: Units = "IMPERIAL";
  @Input() selectedCabinetId: string | null = null;
  @Input() violations: Violation[] = [];
  @Input() cabinetLabels = new Map<string, string>();
  @Input() readOnly = false;

  @Output() designChange = new EventEmitter<Design>();
  @Output() selectCabinet = new EventEmitter<string | null>();

  newTemplateCode = "BASE_DOOR";
  newWallId = "";

  ngOnChanges() {
    if (!this.design.walls.some(wall => wall.id === this.newWallId)) {
      this.newWallId = this.design.walls[0]?.id ?? "";
    }
  }

  /**
   * Cabinets in label order (C1, C2, …), which follows the Room.
   */
  get orderedCabinets(): Cabinet[] {
    const order = (cabinet: Cabinet) => Number(this.cabinetLabels.get(cabinet.id)?.slice(1) ?? Number.MAX_SAFE_INTEGER);
    return [...this.design.cabinets].sort((first, second) => order(first) - order(second));
  }

  get selected(): Cabinet | undefined {
    return this.design.cabinets.find(cabinet => cabinet.id === this.selectedCabinetId);
  }

  get selectedTemplate(): CabinetTemplate | undefined {
    return this.templates.find(template => template.code === this.selected?.templateCode);
  }

  wallNumber(wallId: string): number {
    return this.design.walls.findIndex(wall => wall.id === wallId) + 1;
  }

  violationsOf(cabinetId: string): Violation[] {
    return this.violations.filter(violation => violation.cabinetId === cabinetId);
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
    this.selectCabinet.emit(result.cabinetId);
  }

  updateSelected(changes: Partial<Cabinet>) {
    this.designChange.emit(updateCabinet(this.design, this.selectedCabinetId!, changes));
  }

  removeSelected() {
    this.designChange.emit(removeCabinet(this.design, this.selectedCabinetId!));
    this.selectCabinet.emit(null);
  }
}
