import { NgFor, NgIf, PercentPipe } from "@angular/common";
import { Component, Input } from "@angular/core";

import { PART_ROLE_LABELS } from "../../util/labels";
import { formatLength, Units } from "../../util/units/units";
import { MaterialNesting, SheetPlacement } from "../model/project-output";

/**
 * Sheet layouts drawn to scale, one diagram per sheet.
 */
@Component({
  selector: "nesting",
  standalone: true,
  imports: [NgFor, NgIf, PercentPipe],
  styleUrls: ["./nesting.component.css"],
  templateUrl: "./nesting.component.html",
})
export class NestingComponent {
  readonly roleLabels = PART_ROLE_LABELS;
  readonly formatLength = formatLength;

  @Input() nesting: MaterialNesting[] = [];
  @Input() units: Units = "IMPERIAL";

  label(placement: SheetPlacement): string {
    return `${placement.cabinetLabel} ${this.roleLabels[placement.role]}`;
  }

  size(placement: SheetPlacement): string {
    return `${formatLength(placement.lengthMm, this.units)} × ${formatLength(placement.widthMm, this.units)}`;
  }
}
