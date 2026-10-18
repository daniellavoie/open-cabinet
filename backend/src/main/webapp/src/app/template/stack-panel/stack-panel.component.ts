import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { Material } from "../../catalog/model/material";
import { Attachment, Face, FrontKind, FrontStack, StackRow } from "../../project/model/design";
import { FRONT_KIND_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { AttachmentEditorComponent } from "../attachment-editor/attachment-editor.component";
import { choices, choiceValue, fromChoice, MaterialChoices, PANEL_ROLES } from "../material-choice";
import { attachStack, STACK_FACES } from "../template-edits";

/**
 * A front stack's properties: the area its Fronts fill, their material, and
 * their rows from the top, each a fixed height or a share of what is left.
 */
@Component({
  selector: "stack-panel",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [AttachmentEditorComponent, FormsModule, LengthInputComponent, NgFor, NgIf],
  styleUrls: ["./stack-panel.component.css"],
  templateUrl: "./stack-panel.component.html",
})
export class StackPanelComponent implements OnChanges {
  readonly faces = STACK_FACES;
  readonly kinds = Object.keys(FRONT_KIND_LABELS) as FrontKind[];
  readonly kindLabels = FRONT_KIND_LABELS;
  readonly choiceValue = choiceValue;

  @Input() stack!: FrontStack;
  @Input() targets: string[] = [];
  @Input() materials: Material[] = [];
  @Input() units: Units = "IMPERIAL";
  @Input() pendingFace: Face | null = null;
  @Input() readOnly = false;

  @Output() stackChange = new EventEmitter<FrontStack>();
  @Output() rename = new EventEmitter<string>();
  @Output() remove = new EventEmitter<void>();
  @Output() pick = new EventEmitter<Face>();

  name = "";
  materialChoices!: MaterialChoices;

  ngOnChanges(changes: SimpleChanges) {
    if (changes["stack"]?.previousValue?.id !== this.stack.id) {
      this.name = this.stack.id;
    }
    if (!changes["materials"]) {
      return;
    }
    this.materialChoices = choices(PANEL_ROLES, ["SHEET"], this.materials);
  }

  get rows(): StackRow[] {
    return this.stack.rows ?? [];
  }

  commitName() {
    if (this.name.trim() && this.name.trim() !== this.stack.id) {
      this.rename.emit(this.name.trim());
    } else {
      this.name = this.stack.id;
    }
  }

  set(changes: Partial<FrontStack>) {
    this.stackChange.emit({ ...this.stack, ...changes });
  }

  setMaterial(value: string) {
    this.set({ material: fromChoice(value) });
  }

  setAttachment(face: Face, attachment: Attachment | null) {
    if (attachment) {
      this.stackChange.emit(attachStack(this.stack, face, attachment));
    }
  }

  addRow() {
    this.set({ rows: [...this.rows, { kind: "DOOR" }] });
  }

  setRow(index: number, changes: Partial<StackRow>) {
    this.set({ rows: this.rows.map((row, at) => at === index ? { ...row, ...changes } : row) });
  }

  /** A row is either a fixed height or a share of what is left. */
  setFixed(index: number, fixed: boolean) {
    this.setRow(index, fixed ? { heightMm: 152.4, weight: null } : { heightMm: null, weight: 1 });
  }

  moveRow(index: number, by: number) {
    const rows = [...this.rows];
    const [row] = rows.splice(index, 1);
    rows.splice(Math.max(0, Math.min(rows.length, index + by)), 0, row);
    this.set({ rows });
  }

  removeRow(index: number) {
    this.set({ rows: this.rows.filter((_, at) => at !== index) });
  }

  setSplit(split: boolean) {
    this.set({ splitAboveMm: split ? 609.6 : null });
  }
}
