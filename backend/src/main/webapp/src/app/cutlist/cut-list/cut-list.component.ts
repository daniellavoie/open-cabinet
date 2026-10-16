import { NgFor, NgIf } from "@angular/common";
import { Component, Input } from "@angular/core";

import { PART_ROLE_LABELS } from "../../util/labels";
import { formatLength, Units } from "../../util/units/units";
import { CutListRow } from "../model/project-output";

@Component({
  selector: "cut-list",
  standalone: true,
  imports: [NgFor, NgIf],
  styleUrls: ["./cut-list.component.css"],
  templateUrl: "./cut-list.component.html",
})
export class CutListComponent {
  readonly roleLabels = PART_ROLE_LABELS;
  readonly formatLength = formatLength;

  @Input() rows: CutListRow[] = [];
  @Input() units: Units = "IMPERIAL";

  /**
   * Edge banding written as length edges + width edges, e.g. "L1 W0".
   */
  banding(row: CutListRow): string {
    if (!row.edgeBandingId) {
      return "—";
    }
    return $localize`:@@cutList.edges:L${row.bandedLengthEdges}:length: l${row.bandedWidthEdges}:width:`;
  }
}
