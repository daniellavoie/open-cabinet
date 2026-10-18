import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { StandardValue, Term } from "../../project/model/design";
import { STANDARD_VALUE_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { choiceValue, fromChoice, MaterialChoices } from "../material-choice";

type TermKind = "mm" | "standard" | "thickness";

/**
 * An offset or a size as a sum: lengths, Construction Standard values and
 * material thicknesses, each added or taken away (or half of it).
 */
@Component({
  selector: "terms-editor",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, LengthInputComponent, NgFor, NgIf],
  styleUrls: ["./terms-editor.component.css"],
  templateUrl: "./terms-editor.component.html",
})
export class TermsEditorComponent {
  readonly standardLabels = STANDARD_VALUE_LABELS;
  readonly standardValues = Object.keys(STANDARD_VALUE_LABELS) as StandardValue[];
  readonly factors = [1, -1, 0.5, -0.5];
  readonly choiceValue = choiceValue;

  @Input() terms: Term[] = [];
  @Input() units: Units = "IMPERIAL";
  /** Materials whose thickness can be a term. */
  @Input() thicknesses: MaterialChoices = { roles: [], catalog: [] };
  @Input() inputId = "";

  @Output() termsChange = new EventEmitter<Term[]>();

  kindOf(term: Term): TermKind {
    return term.standard ? "standard" : term.thickness ? "thickness" : "mm";
  }

  factorLabel(factor: number): string {
    return factor === 1 ? "+" : factor === -1 ? "−" : factor === 0.5 ? "+ ½" : "− ½";
  }

  add() {
    this.termsChange.emit([...this.terms, { mm: 0 }]);
  }

  remove(index: number) {
    this.termsChange.emit(this.terms.filter((_, at) => at !== index));
  }

  setKind(index: number, kind: TermKind) {
    const factor = this.terms[index].factor;
    const term: Term = kind === "mm" ? { mm: 0 } : kind === "standard" ? { standard: "DOOR_GAP" } : { thickness: { role: "CARCASS" } };
    this.replace(index, factor != null && factor !== 1 ? { ...term, factor } : term);
  }

  set(index: number, changes: Partial<Term>) {
    this.replace(index, { ...this.terms[index], ...changes });
  }

  setThickness(index: number, value: string) {
    this.set(index, { thickness: fromChoice(value) });
  }

  setFactor(index: number, factor: number) {
    const { factor: _, ...term } = this.terms[index];
    this.replace(index, factor === 1 ? term : { ...term, factor });
  }

  private replace(index: number, term: Term) {
    this.termsChange.emit(this.terms.map((current, at) => at === index ? term : current));
  }
}
