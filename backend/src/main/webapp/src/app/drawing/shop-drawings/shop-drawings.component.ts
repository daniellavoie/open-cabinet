import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input, OnChanges } from "@angular/core";
import { finalize, tap } from "rxjs";

import { Project } from "../../project/model/project";
import { saveBlob } from "../../util/download";
import { DrawingService, DrawingView } from "../drawing-service";

/**
 * The Shop Drawings of the saved Project, with the printable PDF.
 */
@Component({
  selector: "shop-drawings",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgFor, NgIf],
  styleUrls: ["./shop-drawings.component.css"],
  templateUrl: "./shop-drawings.component.html",
})
export class ShopDrawingsComponent implements OnChanges {
  /** The Project as saved: drawings follow what is saved, not the draft. */
  @Input() project!: Project;
  @Input() dirty = false;

  drawings: { title: string; src: string; wide: boolean }[] = [];
  loading = false;
  downloading = false;
  error = "";

  constructor(private drawingService: DrawingService) {}

  ngOnChanges() {
    this.loading = true;
    this.error = "";
    this.drawingService.shopDrawings(this.project.id)
      .pipe(tap(drawings => this.drawings = drawings.map(drawing => ({ title: drawing.title, src: svgSource(drawing), wide: drawing.kind !== "CABINET" }))))
      .pipe(finalize(() => this.loading = false))
      .subscribe({ error: () => this.error = $localize`:@@drawings.error:Impossible de préparer les dessins.` });
  }

  downloadPdf() {
    this.downloading = true;
    this.drawingService.shopDrawingsPdf(this.project.id)
      .pipe(tap(blob => saveBlob(blob, `${this.project.name} — ${$localize`:@@drawings.fileName:dessins d'atelier`}.pdf`)))
      .pipe(finalize(() => this.downloading = false))
      .subscribe({ error: () => this.error = $localize`:@@drawings.pdfError:Impossible de télécharger les dessins.` });
  }
}

/**
 * The SVG as an image source: shown through an image, it cannot run scripts.
 */
function svgSource(drawing: DrawingView): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(drawing.svg)}`;
}

