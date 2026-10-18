import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { finalize, tap } from "rxjs";

import { CabinetTemplate } from "../../project/model/design";
import { ProjectService } from "../../project/project-service";
import { latestVersions, templateName } from "../../project/template-versions";
import { ErrorResponse } from "../../util/model/error-response";
import { ProjectTemplate, ProjectTemplateService } from "../project-template-service";

/**
 * The Shop library: templates every Project of the Shop can use. A published
 * template is archived rather than deleted, so Cabinets using it keep it.
 */
@Component({
  selector: "template-library",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, RouterLink],
  styleUrls: ["./template-library.component.css"],
  templateUrl: "./template-library.component.html",
})
export class TemplateLibraryComponent {
  templates?: ProjectTemplate[];
  /** Starter and library versions a new template can start from. */
  sources: CabinetTemplate[] = [];
  private starters: CabinetTemplate[] = [];
  name = "";
  sourceCode = "BASE_DOOR";
  saving = false;
  error = "";

  constructor(
    private router: Router,
    private templateService: ProjectTemplateService,
    projectService: ProjectService,
  ) {
    projectService.templates()
      .pipe(tap(starters => {
        this.starters = starters;
        this.refreshSources();
      }))
      .subscribe();
    this.load();
  }

  load() {
    this.templateService.findShared()
      .pipe(tap(templates => {
        this.templates = templates;
        this.refreshSources();
      }))
      .subscribe();
  }

  sourceName(template: CabinetTemplate): string {
    return template.name ?? templateName(this.starters, template.code);
  }

  create() {
    const source = this.sources.find(template => template.code === this.sourceCode);
    if (!source || !this.name.trim()) {
      return;
    }
    this.saving = true;
    this.error = "";
    this.templateService.createInLibrary({ name: this.name.trim(), templateCode: source.code, templateVersion: source.version })
      .pipe(finalize(() => this.saving = false))
      .subscribe({
        next: template => this.router.navigate(["/templates", template.id]),
        error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
          $localize`:@@library.createError:Impossible de créer le modèle.`),
      });
  }

  setArchived(template: ProjectTemplate, archived: boolean) {
    this.error = "";
    this.templateService.setArchived(template.id, archived)
      .pipe(tap(() => this.load()))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@library.archiveError:Impossible de modifier le modèle.`) });
  }

  delete(template: ProjectTemplate) {
    this.error = "";
    this.templateService.delete(template.id)
      .pipe(tap(() => this.load()))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@library.deleteError:Impossible de supprimer le modèle.`) });
  }

  private refreshSources() {
    const published = (this.templates ?? []).filter(template => !template.archived && template.versions.length > 0)
      .map(template => template.versions[template.versions.length - 1]);
    this.sources = [...latestVersions(this.starters), ...published];
  }

  private errorOf(response: HttpErrorResponse, fallback: string): string {
    return (response.error as ErrorResponse | null)?.message ?? fallback;
  }
}
