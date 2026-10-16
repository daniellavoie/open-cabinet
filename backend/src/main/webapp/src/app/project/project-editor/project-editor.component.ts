import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { finalize, forkJoin, map, switchMap, tap } from "rxjs";

import { ClientService } from "../../client/client-service";
import { Client } from "../../client/model/client";
import { BillOfMaterialsComponent } from "../../cutlist/bill-of-materials/bill-of-materials.component";
import { CutListComponent } from "../../cutlist/cut-list/cut-list.component";
import { ProjectOutput } from "../../cutlist/model/project-output";
import { NestingComponent } from "../../cutlist/nesting/nesting.component";
import { MemberService } from "../../member/member-service";
import { QuotesComponent } from "../../quote/quotes/quotes.component";
import { ErrorResponse } from "../../util/model/error-response";
import { MM_PER_INCH } from "../../util/units/units";
import { checkConstructionRules } from "../construction-rules";
import { updateCabinet } from "../design-edits";
import { DesignPanelComponent } from "../design-panel/design-panel.component";
import { CabinetTemplate, Design, Violation } from "../model/design";
import { Project } from "../model/project";
import { ProjectService } from "../project-service";
import { snapOffset } from "../room-layout";
import { CabinetMove, RoomViewComponent } from "../room-view/room-view.component";

type Tab = "design" | "cutList" | "nesting" | "materials" | "quotes";

const SNAP_DISTANCE_MM = 30;

/**
 * Designs a Project. Edits apply to a draft checked live against the
 * Construction Rules; saving sends it to the server, which checks again and
 * has the final word (docs/adr/0002-construction-rules-run-in-browser-and-server.md).
 */
@Component({
  selector: "project-editor",
  standalone: true,
  imports: [
    BillOfMaterialsComponent,
    CutListComponent,
    DesignPanelComponent,
    FormsModule,
    NestingComponent,
    NgFor,
    NgIf,
    QuotesComponent,
    RoomViewComponent,
    RouterLink,
  ],
  styleUrls: ["./project-editor.component.css"],
  templateUrl: "./project-editor.component.html",
})
export class ProjectEditorComponent {
  readonly tabs: { id: Tab; label: string }[] = [
    { id: "design", label: $localize`:@@editor.design:Conception` },
    { id: "cutList", label: $localize`:@@editor.cutList:Liste de coupe` },
    { id: "nesting", label: $localize`:@@editor.nesting:Optimisation` },
    { id: "materials", label: $localize`:@@editor.materials:Matériaux` },
    { id: "quotes", label: $localize`:@@editor.quotes:Soumissions` },
  ];

  saved?: Project;
  draft?: Project;
  templates: CabinetTemplate[] = [];
  clients: Client[] = [];
  violations: Violation[] = [];
  invalidCabinetIds = new Set<string>();
  cabinetLabels = new Map<string, string>();
  selectedCabinetId: string | null = null;
  tab: Tab = "design";
  output?: ProjectOutput;
  loadingOutput = false;
  saving = false;
  error = "";

  constructor(
    route: ActivatedRoute,
    public memberService: MemberService,
    private projectService: ProjectService,
    clientService: ClientService,
  ) {
    route.paramMap
      .pipe(map(paramMap => paramMap.get("projectId")!))
      .pipe(switchMap(projectId => forkJoin([
        this.projectService.findById(projectId),
        this.projectService.templates(),
        clientService.findAll(),
      ])))
      .pipe(tap(([project, templates, clients]) => {
        this.templates = templates;
        this.clients = clients;
        this.saved = project;
        this.setDraft(structuredClone(project));
      }))
      .subscribe();
  }

  get units() {
    return this.memberService.shop()!.units;
  }

  get dirty(): boolean {
    return !!this.saved && JSON.stringify(this.saved) !== JSON.stringify(this.draft);
  }

  setDraft(project: Project) {
    this.draft = project;
    this.violations = checkConstructionRules(project.design, this.templates);
    this.invalidCabinetIds = new Set(this.violations.map(violation => violation.cabinetId).filter(id => id !== null));
    this.cabinetLabels = readingOrderLabels(project.design);
    if (this.selectedCabinetId && !project.design.cabinets.some(cabinet => cabinet.id === this.selectedCabinetId)) {
      this.selectedCabinetId = null;
    }
  }

  setDesign(design: Design) {
    this.setDraft({ ...this.draft!, design });
  }

  onMove(move: CabinetMove) {
    const design = this.draft!.design;
    const cabinet = design.cabinets.find(item => item.id === move.cabinetId);
    const wall = design.walls.find(item => item.id === cabinet?.wallId);
    if (!cabinet || !wall) {
      return;
    }
    const step = this.units === "IMPERIAL" ? MM_PER_INCH / 2 : 5;
    const offsetMm = snapOffset(cabinet, move.offsetMm, wall, design.cabinets, step, SNAP_DISTANCE_MM);
    if (offsetMm !== cabinet.offsetMm) {
      this.setDesign(updateCabinet(design, cabinet.id, { offsetMm }));
    }
  }

  selectTab(tab: Tab) {
    this.tab = tab;
    if (tab !== "design" && tab !== "quotes" && !this.dirty) {
      this.loadOutput();
    }
  }

  revert() {
    this.setDraft(structuredClone(this.saved!));
    this.error = "";
  }

  save() {
    const draft = this.draft!;
    this.saving = true;
    this.error = "";
    this.projectService.update(draft.id, {
      clientId: draft.clientId,
      name: draft.name,
      design: draft.design,
      labourMethod: draft.labourMethod,
      labourHours: draft.labourHours,
      manualLines: draft.manualLines,
    })
      .pipe(tap(project => {
        this.saved = project;
        this.setDraft(structuredClone(project));
        this.output = undefined;
        if (this.tab !== "design" && this.tab !== "quotes") {
          this.loadOutput();
        }
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.onSaveError(response) });
  }

  private onSaveError(response: HttpErrorResponse) {
    const body = response.error as ErrorResponse | null;
    if (response.status === 422 && body?.details) {
      this.violations = body.details as Violation[];
      this.invalidCabinetIds = new Set(this.violations.map(violation => violation.cabinetId).filter(id => id !== null));
      this.error = $localize`:@@editor.rulesBroken:Le serveur a refusé la conception : corrigez les règles de construction.`;
    } else {
      this.error = body?.message ?? $localize`:@@editor.saveError:Impossible d'enregistrer le projet.`;
    }
  }

  private loadOutput() {
    this.loadingOutput = true;
    this.projectService.output(this.saved!.id)
      .pipe(tap(output => this.output = output))
      .pipe(finalize(() => this.loadingOutput = false))
      .subscribe({ error: () => this.error = $localize`:@@editor.outputError:Impossible de calculer la liste de coupe.` });
  }
}

/**
 * Same labels as the server's Cut List (C1, C2, …): by Wall, then along it,
 * then from the floor up.
 */
export function readingOrderLabels(design: Design): Map<string, string> {
  const wallIndex = new Map(design.walls.map((wall, index) => [wall.id, index]));
  const ordered = [...design.cabinets].sort((first, second) =>
    (wallIndex.get(first.wallId) ?? Number.MAX_SAFE_INTEGER) - (wallIndex.get(second.wallId) ?? Number.MAX_SAFE_INTEGER)
    || first.offsetMm - second.offsetMm
    || first.elevationMm - second.elevationMm);
  return new Map(ordered.map((cabinet, index) => [cabinet.id, `C${index + 1}`]));
}
