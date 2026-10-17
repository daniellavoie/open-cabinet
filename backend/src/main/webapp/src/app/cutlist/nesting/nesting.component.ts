import { NgFor, NgIf, PercentPipe } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

import { PART_ROLE_LABELS } from "../../util/labels";
import { formatLength, Units } from "../../util/units/units";
import { MaterialNesting, NestingStrategy, SheetPlacement } from "../model/project-output";

const STRIP_DIRECTIONS: Record<NestingStrategy["stripDirection"], string> = {
  ALONG: $localize`:@@nesting.stripsAlong:Bandes en long`,
  ACROSS: $localize`:@@nesting.stripsAcross:Bandes en travers`,
};

const ORDERS: Record<NestingStrategy["order"], string> = {
  WIDTH: $localize`:@@nesting.widestFirst:plus larges d'abord`,
  LENGTH: $localize`:@@nesting.longestFirst:plus longues d'abord`,
  AREA: $localize`:@@nesting.largestFirst:plus grandes d'abord`,
};

const ORIENTATIONS: Record<NestingStrategy["orientation"], string> = {
  LONG_ALONG: $localize`:@@nesting.grainlessAlong:pièces sans fil en long`,
  LONG_ACROSS: $localize`:@@nesting.grainlessAcross:pièces sans fil en travers`,
};

/**
 * Sheet layouts drawn to scale, one diagram per sheet. Each material shows
 * the layout chosen for buying, and the next best ones to compare.
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

  @Input() nesting: MaterialNesting[] = [];
  @Input() units: Units = "IMPERIAL";

  /** The layout shown per material: 0 for the chosen one, then its alternatives. */
  shown = new Map<string, number>();

  layouts(material: MaterialNesting): MaterialNesting[] {
    return [material, ...material.alternatives ?? []];
  }

  layout(material: MaterialNesting): MaterialNesting {
    return this.layouts(material)[this.shown.get(material.materialId) ?? 0] ?? material;
  }

  describe(strategy: NestingStrategy | undefined): string {
    return strategy ? `${STRIP_DIRECTIONS[strategy.stripDirection]} · ${ORDERS[strategy.order]} · ${ORIENTATIONS[strategy.orientation]}` : "";
  }

  label(placement: SheetPlacement): string {
    return `${placement.cabinetLabel} ${this.roleLabels[placement.role]}`;
  }

  size(placement: SheetPlacement): string {
    return `${formatLength(placement.lengthMm, this.units)} × ${formatLength(placement.widthMm, this.units)}`;
  }
}
