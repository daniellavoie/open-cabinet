import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

import { PART_ROLE_LABELS } from "../../util/labels";
import { formatLength, Units } from "../../util/units/units";
import { CutListRow, OrderedFrontRow } from "../model/project-output";

@Component({
  selector: "cut-list",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgFor, NgIf],
  styleUrls: ["./cut-list.component.css"],
  templateUrl: "./cut-list.component.html",
})
export class CutListComponent {
  readonly roleLabels = PART_ROLE_LABELS;
  readonly formatLength = formatLength;

  @Input() rows: CutListRow[] = [];
  /** Fronts ordered from a supplier rather than cut. */
  @Input() orderedFronts: OrderedFrontRow[] = [];
  @Input() units: Units = "IMPERIAL";

  /**
   * Banded edges as "length edges / width edges", e.g. "1 / 0".
   */
  banding(row: CutListRow): string {
    return row.edgeBandingId ? `${row.bandedLengthEdges} / ${row.bandedWidthEdges}` : "—";
  }
}
