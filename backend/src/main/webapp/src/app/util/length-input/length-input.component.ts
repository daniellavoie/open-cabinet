import { Component, EventEmitter, Input, OnChanges, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { formatLength, parseLength, Units } from "../units/units";

/**
 * A text box for a length in the Shop's units, holding the value in millimetres.
 */
@Component({
  selector: "length-input",
  standalone: true,
  imports: [FormsModule],
  styleUrls: ["./length-input.component.css"],
  templateUrl: "./length-input.component.html",
})
export class LengthInputComponent implements OnChanges {
  @Input() valueMm: number | null = null;
  @Input() units: Units = "IMPERIAL";
  @Input() inputId = "";
  @Input() disabled = false;
  @Output() valueMmChange = new EventEmitter<number>();

  text = "";
  invalid = false;

  ngOnChanges() {
    this.text = formatLength(this.valueMm, this.units);
    this.invalid = false;
  }

  commit() {
    const mm = parseLength(this.text, this.units);
    if (mm === null) {
      this.invalid = true;
      return;
    }
    this.invalid = false;
    this.text = formatLength(mm, this.units);
    if (mm !== this.valueMm) {
      this.valueMmChange.emit(mm);
    }
  }
}
