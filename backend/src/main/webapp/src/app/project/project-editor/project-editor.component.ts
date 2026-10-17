import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, OnDestroy } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { finalize, forkJoin, interval, map, Subscription, switchMap, tap } from "rxjs";

import { CatalogService } from "../../catalog/catalog-service";
import { Material } from "../../catalog/model/material";
import { ClientService } from "../../client/client-service";
import { ShopCabinetTemplate, ShopTemplateService } from "../../template/shop-template-service";
import { Client } from "../../client/model/client";
import { BillOfMaterialsComponent } from "../../cutlist/bill-of-materials/bill-of-materials.component";
import { CutListComponent } from "../../cutlist/cut-list/cut-list.component";
import { ProjectOutput } from "../../cutlist/model/project-output";
import { NestingComponent } from "../../cutlist/nesting/nesting.component";
import { MemberService } from "../../member/member-service";
import { QuotesComponent } from "../../quote/quotes/quotes.component";
import { TEMPLATE_LABELS, VIOLATION_LABELS } from "../../util/labels";
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
import { newerVersions, outdatedCabinets, RefusedChange, updateToLatest } from "../template-versions";

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
  readonly templateLabels = TEMPLATE_LABELS;
  readonly violationLabels = VIOLATION_LABELS;

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
  shopTemplates: ShopCabinetTemplate[] = [];
  message = "";
  violations: Violation[] = [];
  invalidIds = new Set<string>();
  /** Cabinets on an older Template Version than their template's newest. */
  outdatedIds = new Set<string>();
  /** What updating the outdated Cabinets brings, per template. */
  pendingVersions: { code: string; versions: CabinetTemplate[] }[] = [];
  showPendingVersions = false;
  /** Cabinets left out of the last "Update all" for breaking Construction Rules. */
  refusedUpdates: RefusedChange[] = [];
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
    private shopTemplateService: ShopTemplateService,
  ) {
    route.paramMap
      .pipe(map(paramMap => paramMap.get("projectId")!))
      .pipe(switchMap(projectId => forkJoin([
        this.projectService.findById(projectId),
        this.projectService.templates(),
        clientService.findAll(),
        catalogService.findAll(),
        shopTemplateService.findAll(),
      ])))
      .pipe(tap(([project, templates, clients, materials, shopTemplates]) => {
        this.shopTemplates = shopTemplates;
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
    this.setOutdated(project.design);
    if (this.selectedId && !onWall(project.design).some(item => item.id === this.selectedId)) {
      this.selectedId = null;
    }
  }

  setDesign(design: Design) {
    this.setDraft({ ...this.draft!, design });
  }

  /**
   * Cabinets snap to each other, to Appliances and to Fillers; Openings,
   * Appliances and Fillers snap to everything on their Wall.
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
    const others = cabinet ? [...design.cabinets, ...design.appliances ?? [], ...design.fillers ?? []] : onWall(design);
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

  /**
   * Moves every outdated Cabinet to its template's newest version; those that
   * would break a Construction Rule keep their version and are listed.
   */
  updateAll() {
    const result = updateToLatest(this.draft!.design, this.templates, [...this.outdatedIds]);
    this.refusedUpdates = result.refused;
    this.setDesign(result.design);
  }

  private setOutdated(design: Design) {
    const outdated = outdatedCabinets(design, this.templates);
    this.outdatedIds = new Set(outdated.map(cabinet => cabinet.id));
    const pending = new Map<string, CabinetTemplate[]>();
    for (const cabinet of outdated) {
      const known = pending.get(cabinet.templateCode) ?? [];
      const newer = newerVersions(this.templates, cabinet).filter(version => !known.includes(version));
      pending.set(cabinet.templateCode, [...known, ...newer].sort((first, second) => first.version - second.version));
    }
    this.pendingVersions = [...pending].map(([code, versions]) => ({ code, versions }));
    if (outdated.length === 0) {
      this.showPendingVersions = false;
    }
  }

  saveTemplate(request: { name: string; cabinet: Cabinet }) {
    this.error = "";
    this.shopTemplateService.create(request.name, request.cabinet)
      .pipe(tap(template => {
        this.shopTemplates = [...this.shopTemplates, template].sort((first, second) => first.name.localeCompare(second.name));
        this.message = $localize`:@@editor.templateSaved:Modèle « ${template.name}:name: » enregistré.`;
      }))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = (response.error as ErrorResponse | null)?.message
        ?? $localize`:@@editor.templateError:Impossible d'enregistrer le modèle.` });
  }

  selectTab(tab: Tab) {
    this.tab = tab;
    if (tab !== "design" && tab !== "quotes" && !this.dirty) {
      this.loadOutput();
    }
  }

  revert() {
    this.refusedUpdates = [];
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
        this.refusedUpdates = [];
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
  return [...design.cabinets, ...design.openings ?? [], ...design.appliances ?? [], ...design.fillers ?? []];
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
