import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { Material } from "../../catalog/model/material";
import { Axis, FrontKind, Hardware } from "../../project/model/design";
import { AXIS_LABELS, FRONT_KIND_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { choices, choiceValue, fromChoice, HARDWARE_ROLES, MaterialChoices } from "../material-choice";

type Mode = "fixed" | "length" | "steps";

/**
 * A template's hardware, each item counted per Cabinet or on a Part or front
 * stack: a fixed number, one per length, or by steps of the host's size.
 */
@Component({
  selector: "hardware-panel",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, LengthInputComponent, NgFor, NgIf],
  styleUrls: ["./hardware-panel.component.css"],
  templateUrl: "./hardware-panel.component.html",
})
export class HardwarePanelComponent implements OnChanges {
  readonly axes: Axis[] = ["X", "Y", "Z"];
  readonly axisLabels = AXIS_LABELS;
  readonly frontKinds: FrontKind[] = ["DOOR", "DRAWER", "FALSE_FRONT"];
  readonly frontKindLabels = FRONT_KIND_LABELS;
  readonly choiceValue = choiceValue;

  @Input() hardware: Hardware[] = [];
  /** Parts hardware can be counted on. */
  @Input() parts: string[] = [];
  /** Front stacks hardware can be counted on. */
  @Input() stacks: string[] = [];
  @Input() materials: Material[] = [];
  @Input() units: Units = "IMPERIAL";
  @Input() readOnly = false;

  @Output() itemChange = new EventEmitter<{ index: number; item: Hardware }>();
  @Output() add = new EventEmitter<void>();
  @Output() remove = new EventEmitter<number>();

  materialChoices!: MaterialChoices;

  ngOnChanges(changes: SimpleChanges) {
    if (changes["materials"]) {
      this.materialChoices = choices(HARDWARE_ROLES, ["HARDWARE"], this.materials);
    }
  }

  modeOf(item: Hardware): Mode {
    return item.quantity.fixed != null ? "fixed" : item.quantity.steps ? "steps" : "length";
  }

  set(index: number, changes: Partial<Hardware>) {
    this.itemChange.emit({ index, item: { ...this.hardware[index], ...changes } });
  }

  setQuantity(index: number, changes: Partial<Hardware["quantity"]>) {
    this.set(index, { quantity: { ...this.hardware[index].quantity, ...changes } });
  }

  setMode(index: number, mode: Mode) {
    this.set(index, {
      quantity: mode === "fixed" ? { fixed: 1 }
        : mode === "length" ? { perMm: 508, axis: "Y", minimum: 2 }
        : { axis: "Y", steps: [{ upToMm: 1000, quantity: 2 }, { quantity: 3 }] },
    });
  }

  setOn(index: number, on: string) {
    this.set(index, { on: on || null, frontKind: this.stacks.includes(on) ? this.hardware[index].frontKind : null });
  }

  setStep(index: number, step: number, changes: { upToMm?: number | null; quantity?: number }) {
    const steps = (this.hardware[index].quantity.steps ?? []).map((current, at) => at === step ? { ...current, ...changes } : current);
    this.setQuantity(index, { steps });
  }

  addStep(index: number) {
    const steps = this.hardware[index].quantity.steps ?? [];
    const last = steps[steps.length - 1];
    // The last step has no limit: a new one goes before it.
    this.setQuantity(index, { steps: [...steps.slice(0, -1), { upToMm: 1000, quantity: last?.quantity ?? 1 }, last ?? { quantity: 1 }] });
  }

  removeStep(index: number, step: number) {
    this.setQuantity(index, { steps: (this.hardware[index].quantity.steps ?? []).filter((_, at) => at !== step) });
  }

  setMaterial(index: number, value: string) {
    this.set(index, { material: fromChoice(value) });
  }
}
