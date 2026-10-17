import { CurrencyPipe, NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, tap } from "rxjs";

import { MemberService } from "../../member/member-service";
import { MATERIAL_KIND_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { formatLength } from "../../util/units/units";
import { CatalogService, MaterialRequest } from "../catalog-service";
import { CatalogImportResult } from "../model/catalog-import-result";
import { Material, MaterialKind } from "../model/material";

@Component({
  selector: "catalog",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, FormsModule, LengthInputComponent, NgFor, NgIf],
  styleUrls: ["./catalog.component.css"],
  templateUrl: "./catalog.component.html",
})
export class CatalogComponent {
  readonly kinds = Object.entries(MATERIAL_KIND_LABELS) as [MaterialKind, string][];
  readonly kindLabels = MATERIAL_KIND_LABELS;
  readonly formatLength = formatLength;

  materials?: Material[];
  editing: Material | null = null;
  form: MaterialRequest = emptyForm();
  saving = false;
  error = "";
  importResult?: CatalogImportResult;

  constructor(
    public memberService: MemberService,
    private catalogService: CatalogService,
  ) {
    this.load();
  }

  load() {
    this.catalogService.findAll()
      .pipe(tap(materials => this.materials = materials))
      .subscribe();
  }

  exportCsv() {
    this.catalogService.exportCsv()
      .pipe(tap(blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "catalogue.csv";
        link.click();
        URL.revokeObjectURL(url);
      }))
      .subscribe();
  }

  importCsv(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = "";
    if (!file) {
      return;
    }
    file.text().then(csv => this.catalogService.importCsv(csv)
      .pipe(tap(result => {
        this.importResult = result;
        if (result.applied) {
          this.load();
        }
      }))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@catalog.importError:Impossible d'importer le fichier.` }));
  }

  materialsOf(kind: MaterialKind): Material[] {
    return (this.materials ?? []).filter(material => material.kind === kind);
  }

  edit(material: Material | null) {
    this.editing = material;
    this.form = material ? { ...material } : emptyForm();
    this.error = "";
  }

  save() {
    this.saving = true;
    this.error = "";
    const request = this.editing
      ? this.catalogService.update(this.editing.id, this.form)
      : this.catalogService.create(this.form);

    request
      .pipe(tap(() => {
        this.edit(null);
        this.load();
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@catalog.saveError:Impossible d'enregistrer le matériau.` });
  }

  remove() {
    this.catalogService.delete(this.editing!.id)
      .pipe(tap(() => {
        this.edit(null);
        this.load();
      }))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@catalog.deleteError:Impossible de retirer le matériau.` });
  }
}

function emptyForm(): MaterialRequest {
  return { kind: "SHEET", name: "", sku: null, price: 0, thicknessMm: null, lengthMm: null, widthMm: null, grain: false };
}
