import { NgFor, NgIf, PercentPipe } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from "@angular/core";

import { NestingLayout } from "../../project/model/project";
import { NESTING_LAYOUT_LABELS, PART_ROLE_LABELS } from "../../util/labels";
import { formatLength, Units } from "../../util/units/units";
import { MaterialNesting, SheetPlacement } from "../model/project-output";

/**
 * Sheet layouts drawn to scale, one diagram per sheet, with each material's layouts compared and
 * the Project's choice of layout.
 */
@Component({
  selector: "nesting",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgFor, NgIf, PercentPipe],
  styleUrls: ["./nesting.component.css"],
  templateUrl: "./nesting.component.html",
})
export class NestingComponent {
  readonly roleLabels = PART_ROLE_LABELS;
  readonly formatLength = formatLength;

  readonly layoutLabels = NESTING_LAYOUT_LABELS;
  readonly layouts: NestingLayout[] = ["RIP_FIRST", "CROSSCUT_FIRST"];

  @Input() nesting: MaterialNesting[] = [];
  @Input() units: Units = "IMPERIAL";
  /** The Project's choice; null keeps the fewest sheets for each material. */
  @Input() layout: NestingLayout | null = null;
  @Input() readOnly = false;

  @Output() layoutChange = new EventEmitter<NestingLayout | null>();

  label(placement: SheetPlacement): string {
    return `${placement.cabinetLabel} ${this.roleLabels[placement.role]}`;
  }

  size(placement: SheetPlacement): string {
    return `${formatLength(placement.lengthMm, this.units)} × ${formatLength(placement.widthMm, this.units)}`;
  }
}
