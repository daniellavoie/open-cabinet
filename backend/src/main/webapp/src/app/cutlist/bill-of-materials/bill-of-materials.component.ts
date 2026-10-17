import { CurrencyPipe, DecimalPipe, NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

import { MATERIAL_KIND_LABELS } from "../../util/labels";
import { BomLine, OperationCounts } from "../model/project-output";

@Component({
  selector: "bill-of-materials",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, NgFor, NgIf],
  styleUrls: ["./bill-of-materials.component.css"],
  templateUrl: "./bill-of-materials.component.html",
})
export class BillOfMaterialsComponent {
  readonly kindLabels = MATERIAL_KIND_LABELS;

  @Input() lines: BomLine[] = [];
  @Input() total = 0;
  @Input() operations?: OperationCounts;
  @Input() currency = "CAD";

  unit(line: BomLine): string {
    switch (line.kind) {
      case "SHEET":
        return $localize`:@@bom.sheets:panneau(x)`;
      case "EDGE_BANDING":
      case "MOULDING":
        return "m";
      case "FRONT":
        return "m²";
      default:
        return $localize`:@@bom.units:unité(s)`;
    }
  }
}
