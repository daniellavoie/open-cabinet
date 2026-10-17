import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, OnDestroy } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { finalize, forkJoin, interval, map, Subscription, switchMap, tap } from "rxjs";

import { CatalogService } from "../../catalog/catalog-service";
import { Material } from "../../catalog/model/material";
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
import { moveDivider, updateCabinet, updateItem } from "../design-edits";
import { DesignPanelComponent } from "../design-panel/design-panel.component";
import { Cabinet, CabinetTemplate, Design, Violation } from "../model/design";
import { Project } from "../model/project";
import { ProjectLock } from "../model/project-lock";
import { ProjectService } from "../project-service";
import { OnWall, snapOffset } from "../room-layout";
import { ItemMove, RoomViewComponent } from "../room-view/room-view.component";

type Tab = "design" | "cutList" | "nesting" | "materials" | "quotes";

const SNAP_DISTANCE_MM = 30;
const LOCK_RENEWAL_MS = 45_000;

/**
 * Designs a Project. Edits apply to a draft checked live against the
 * Construction Rules; saving sends it to the server, which checks again and
 * has the final word (docs/adr/0002-construction-rules-run-in-browser-and-server.md).
 * One Member edits at a time: the editor holds the Project's lock while open,
 * and is read-only for everyone else.
 */
@Component({
  selector: "project-editor",
  changeDetection: ChangeDetectionStrategy.Eager,
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
export class ProjectEditorComponent implements OnDestroy {
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
  /** The Shop's ordered Front styles. */
  frontStyles: Material[] = [];
  violations: Violation[] = [];
  invalidIds = new Set<string>();
  cabinetLabels = new Map<string, string>();
  /**
   * The selected Cabinet, Opening or Appliance.
   */
  selectedId: string | null = null;
  tab: Tab = "design";
  output?: ProjectOutput;
  loadingOutput = false;
  saving = false;
  error = "";
  lock?: ProjectLock;
  private renewal?: Subscription;

  constructor(
    route: ActivatedRoute,
    public memberService: MemberService,
    private projectService: ProjectService,
    clientService: ClientService,
    catalogService: CatalogService,
  ) {
    route.paramMap
      .pipe(map(paramMap => paramMap.get("projectId")!))
      .pipe(switchMap(projectId => forkJoin([
        this.projectService.findById(projectId),
        this.projectService.templates(),
        clientService.findAll(),
        catalogService.findAll(),
      ])))
      .pipe(tap(([project, templates, clients, materials]) => {
        this.templates = templates;
        this.clients = clients;
        this.frontStyles = materials.filter(material => material.kind === "FRONT");
        this.saved = project;
        this.setDraft(structuredClone(project));
        this.takeLock();
      }))
      .subscribe();
  }

  ngOnDestroy() {
    this.renewal?.unsubscribe();
    if (this.lock?.mine && this.saved) {
      this.projectService.unlock(this.saved.id).subscribe();
    }
  }

  get readOnly(): boolean {
    return !this.lock?.mine;
  }

  /**
   * Takes the lock and keeps renewing it; drops to read-only if it is lost.
   */
  takeLock() {
    this.renewal?.unsubscribe();
    this.projectService.lock(this.saved!.id)
      .pipe(tap(lock => {
        this.lock = lock;
        if (lock.mine) {
          this.renewal = interval(LOCK_RENEWAL_MS)
            .pipe(switchMap(() => this.projectService.lock(this.saved!.id)))
            .pipe(tap(renewed => {
              this.lock = renewed;
              if (!renewed.mine) {
                this.renewal?.unsubscribe();
              }
            }))
            .subscribe();
        }
      }))
      .subscribe();
  }

  get units() {
    return this.memberService.shop()!.units;
  }

  get dirty(): boolean {
    if (this.readOnly) {
      return false;
    }
    return !!this.saved && JSON.stringify(this.saved) !== JSON.stringify(this.draft);
  }

  setDraft(project: Project) {
    this.draft = project;
    this.setViolations(checkConstructionRules(project.design, this.templates));
    this.cabinetLabels = readingOrderLabels(project.design);
    if (this.selectedId && !onWall(project.design).some(item => item.id === this.selectedId)) {
      this.selectedId = null;
    }
  }

  setDesign(design: Design) {
    this.setDraft({ ...this.draft!, design });
  }

  /**
   * Cabinets snap to each other and to Appliances; Openings and Appliances
   * snap to everything on their Wall.
   */
  onMove(move: ItemMove) {
    const design = this.draft!.design;
    const cabinet = design.cabinets.find(item => item.id === move.id);
    if (cabinet && move.dividerIndex !== undefined) {
      this.onDividerMove(cabinet, move.dividerIndex, move.offsetMm);
      return;
    }
    const item = cabinet ?? onWall(design).find(candidate => candidate.id === move.id);
    const wall = design.walls.find(candidate => candidate.id === item?.wallId);
    if (!item || !wall) {
      return;
    }
    const step = this.units === "IMPERIAL" ? MM_PER_INCH / 2 : 5;
    const others = cabinet ? [...design.cabinets, ...design.appliances ?? []] : onWall(design);
    const offsetMm = snapOffset(item, move.offsetMm, wall, others, step, SNAP_DISTANCE_MM);
    if (offsetMm !== item.offsetMm) {
      this.setDesign(cabinet ? updateCabinet(design, item.id, { offsetMm }) : updateItem(design, item.id, { offsetMm }));
    }
  }

  /**
   * Dividers snap to the grid; the Construction Rules flag sections left too narrow.
   */
  private onDividerMove(cabinet: Cabinet, index: number, positionMm: number) {
    const step = this.units === "IMPERIAL" ? MM_PER_INCH / 4 : 5;
    const clamped = Math.min(Math.max(positionMm, 0), cabinet.widthMm);
    const snapped = Math.round(Math.round(clamped / step) * step * 10) / 10;
    if (snapped !== (cabinet.dividerPositionsMm ?? [])[index]) {
      this.setDesign(moveDivider(this.draft!.design, cabinet.id, index, snapped));
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
      this.setViolations(body.details as Violation[]);
      this.error = $localize`:@@editor.rulesBroken:Le serveur a refusé la conception : corrigez les règles de construction.`;
    } else if (response.status === 409) {
      this.error = $localize`:@@editor.lockLost:Quelqu'un d'autre modifie ce projet : vos changements n'ont pas été enregistrés.`;
      this.takeLock();
    } else {
      this.error = body?.message ?? $localize`:@@editor.saveError:Impossible d'enregistrer le projet.`;
    }
  }

  private setViolations(violations: Violation[]) {
    this.violations = violations;
    this.invalidIds = new Set(violations.map(violation => violation.cabinetId ?? violation.itemId ?? null).filter(id => id !== null));
  }

  private loadOutput() {
    this.loadingOutput = true;
    this.projectService.output(this.saved!.id)
      .pipe(tap(output => this.output = output))
      .pipe(finalize(() => this.loadingOutput = false))
      .subscribe({ error: () => this.error = $localize`:@@editor.outputError:Impossible de calculer la liste de coupe.` });
  }
}

function onWall(design: Design): OnWall[] {
  return [...design.cabinets, ...design.openings ?? [], ...design.appliances ?? []];
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
