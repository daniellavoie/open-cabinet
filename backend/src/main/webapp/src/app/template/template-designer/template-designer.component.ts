import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { finalize, forkJoin, map, switchMap, tap } from "rxjs";

import { CatalogService } from "../../catalog/catalog-service";
import { Material } from "../../catalog/model/material";
import { MemberService } from "../../member/member-service";
import { assemble, AssemblyResult, CABINET, faceAxis, materialIdOf } from "../../project/assembly";
import { AssemblyPart, CabinetTemplate, CabinetType, Face, FrontStack, Hardware, MaterialRef, TemplateAssembly } from "../../project/model/design";
import { checkTemplate, TemplateSize, TemplateViolation } from "../../project/template-rules";
import { CABINET_TYPE_LABELS, TEMPLATE_RULE_LABELS, TEMPLATE_SIZE_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { ErrorResponse } from "../../util/model/error-response";
import { HardwarePanelComponent } from "../hardware-panel/hardware-panel.component";
import { PartPanelComponent } from "../part-panel/part-panel.component";
import { ProjectTemplate, ProjectTemplateService } from "../project-template-service";
import { StackPanelComponent } from "../stack-panel/stack-panel.component";
import {
  addHardware, addMoulding, addPanel, addStack, attach, attachStack, partNamed, remove, removeHardware, rename, setDivider,
  updateHardware, updatePart, updateStack,
} from "../template-edits";
import { PickedFace, TemplateViewComponent } from "../template-view/template-view.component";

type Dimension = "Width" | "Height" | "Depth";

/**
 * Designs one of a Project's Cabinet Templates (ADR 0006): its size ranges and
 * shelves, its Parts, front stacks and hardware, drawn at its minimum, default
 * or maximum size with the template rules checked live. Saving keeps the
 * draft; publishing makes it the next Template Version Cabinets can use.
 */
@Component({
  selector: "template-designer",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, HardwarePanelComponent, LengthInputComponent, NgFor, NgIf, PartPanelComponent, RouterLink,
    StackPanelComponent, TemplateViewComponent],
  styleUrls: ["./template-designer.component.css"],
  templateUrl: "./template-designer.component.html",
})
export class TemplateDesignerComponent {
  readonly typeLabels = CABINET_TYPE_LABELS;
  readonly types = Object.keys(CABINET_TYPE_LABELS) as CabinetType[];
  readonly sizeLabels = TEMPLATE_SIZE_LABELS;
  readonly sizes: TemplateSize[] = ["MINIMUM", "DEFAULT", "MAXIMUM"];
  readonly ruleLabels = TEMPLATE_RULE_LABELS;
  readonly dimensions: { key: Dimension; label: string }[] = [
    { key: "Width", label: $localize`:@@designer.width:Largeur` },
    { key: "Height", label: $localize`:@@designer.height:Hauteur` },
    { key: "Depth", label: $localize`:@@designer.depth:Profondeur` },
  ];

  template?: ProjectTemplate;
  draft?: CabinetTemplate;
  name = "";
  materials: Material[] = [];
  /** The template or front stack selected. */
  selected: string | null = null;
  /** The selected item's face waiting for a target picked in 3D. */
  pending: Face | null = null;
  size: TemplateSize = "DEFAULT";
  result: AssemblyResult = { parts: [], hardware: [], violations: [] };
  violations: TemplateViolation[] = [];
  saving = false;
  changeNote = "";
  publishing = false;
  /** After the first version: offer it to the Cabinet the template was made from. */
  offerSwitch = false;
  message = "";
  error = "";
  private savedJson = "";

  constructor(
    route: ActivatedRoute,
    private router: Router,
    public memberService: MemberService,
    private templateService: ProjectTemplateService,
    catalogService: CatalogService,
  ) {
    route.paramMap
      .pipe(map(paramMap => paramMap.get("templateId")!))
      .pipe(switchMap(templateId => forkJoin([templateService.findById(templateId), catalogService.findAll()])))
      .pipe(tap(([template, materials]) => {
        this.materials = materials;
        this.load(template);
      }))
      .subscribe({ error: () => this.error = $localize`:@@designer.loadError:Impossible d'ouvrir le modèle.` });
  }

  get units() {
    return this.memberService.shop()!.units;
  }

  get assembly(): TemplateAssembly {
    return this.draft!.assembly ?? {};
  }

  get dirty(): boolean {
    return !!this.draft && JSON.stringify({ name: this.name, draft: this.draft }) !== this.savedJson;
  }

  get latestVersion(): number {
    return this.template?.versions.length ?? 0;
  }

  /**
   * Lists derived from the draft, computed once per change: handed to child
   * panels as inputs, they must not be new arrays on every check.
   */
  items: { id: string; kind: "part" | "stack" | "divider"; label: string }[] = [];
  partNames: string[] = [];
  stackNames: string[] = [];
  hardware: Hardware[] = [];
  /** For each Part or front stack, the Parts its faces can be held against: not itself, not repeated Parts. */
  targets: Record<string, string[]> = {};

  get selectedStack(): FrontStack | null {
    return (this.assembly.frontStacks ?? []).find(stack => stack.id === this.selected) ?? null;
  }

  get selectedIsDivider(): boolean {
    return !!this.selected && this.assembly.divider?.id === this.selected;
  }

  targetsFor(id: string): string[] {
    return this.targets[id] ?? [];
  }

  partOf(id: string) {
    return partNamed(this.assembly, id);
  }

  problemsOf(id: string): number {
    return this.violations.filter(violation => violation.partId === id).length;
  }

  private load(template: ProjectTemplate) {
    this.template = template;
    this.name = template.name;
    this.setDraft(structuredClone(template.draft));
    this.savedJson = JSON.stringify({ name: this.name, draft: this.draft });
  }

  /** Every change goes through here: the preview and the rules follow. */
  setDraft(draft: CabinetTemplate) {
    this.draft = draft;
    this.refresh();
  }

  setAssembly(assembly: TemplateAssembly) {
    this.setDraft({ ...this.draft!, assembly });
  }

  set(changes: Partial<CabinetTemplate>) {
    this.setDraft({ ...this.draft!, ...changes });
  }

  setRange(dimension: Dimension, size: TemplateSize, valueMm: number) {
    const key = `${size === "MINIMUM" ? "min" : size === "DEFAULT" ? "default" : "max"}${dimension}Mm` as keyof CabinetTemplate;
    this.set({ [key]: valueMm } as Partial<CabinetTemplate>);
  }

  range(dimension: Dimension, size: TemplateSize): number {
    const key = `${size === "MINIMUM" ? "min" : size === "DEFAULT" ? "default" : "max"}${dimension}Mm` as keyof CabinetTemplate;
    return this.draft![key] as number;
  }

  setSize(size: TemplateSize) {
    this.size = size;
    this.refresh();
  }

  private refresh() {
    const assembly = this.assembly;
    const divider = assembly.divider;
    this.items = [
      ...(assembly.parts ?? []).map(part => ({ id: part.id, kind: "part" as const, label: part.id })),
      ...(assembly.frontStacks ?? []).map(stack => ({ id: stack.id, kind: "stack" as const, label: stack.id })),
      ...(divider ? [{ id: divider.id, kind: "divider" as const, label: divider.id }] : []),
    ];
    this.partNames = [...(assembly.parts ?? []).map(part => part.id), ...(divider ? [divider.id] : [])];
    this.stackNames = (assembly.frontStacks ?? []).map(stack => stack.id);
    this.hardware = assembly.hardware ?? [];
    const holding = (assembly.parts ?? []).filter(part => !part.repeat).map(part => part.id);
    this.targets = Object.fromEntries(this.items.map(item => [item.id, holding.filter(name => name !== item.id)]));

    const standard = this.memberService.shop()!.settings.constructionStandard;
    const byId = new Map(this.materials.map(material => [material.id, material]));
    const material = (reference: MaterialRef) => byId.get(materialIdOf(reference, standard) ?? "");
    const shop = {
      standard: { DOOR_GAP: standard.doorGapMm, TOE_KICK_HEIGHT: standard.toeKickHeightMm },
      thickness: (reference: MaterialRef) => material(reference)?.thicknessMm ?? 0,
      sheet: (reference: MaterialRef) => {
        const found = material(reference);
        return found?.kind === "SHEET" && found.lengthMm && found.widthMm
          ? { lengthMm: found.lengthMm, widthMm: found.widthMm, grain: found.grain } : null;
      },
      trimMm: standard.sheetTrimMm,
    };
    const draft = this.draft!;
    this.result = assemble(draft.assembly ?? {}, {
      width: this.range("Width", this.size), height: this.range("Height", this.size), depth: this.range("Depth", this.size),
      shelfCount: draft.defaultShelfCount, dividers: draft.assembly?.divider ? [this.range("Width", this.size) / 2] : [],
      standard: shop.standard, thickness: shop.thickness,
    });
    this.violations = checkTemplate(draft, shop);
  }

  select(id: string | null) {
    this.selected = id;
    this.pending = null;
  }

  add(kind: "carcass" | "back" | "frontPanel" | "stack" | "moulding") {
    const name = $localize`:@@designer.newPart:Pièce`;
    const result = kind === "stack" ? addStack(this.assembly)
      : kind === "moulding" ? addMoulding(this.assembly, name)
      : addPanel(this.assembly, kind === "carcass" ? "CARCASS" : kind === "back" ? "BACK" : "FRONT", name);
    this.setAssembly(result.assembly);
    this.select(result.id);
  }

  setDivider(enabled: boolean) {
    this.setAssembly(setDivider(this.assembly, enabled));
  }

  renameSelected(name: string) {
    const renamed = rename(this.assembly, this.selected!, name);
    if (renamed === this.assembly) {
      this.error = $localize`:@@designer.nameTaken:Ce nom est vide ou déjà pris.`;
      return;
    }
    this.error = "";
    this.setAssembly(renamed);
    this.selected = name.trim();
  }

  removeSelected() {
    this.setAssembly(remove(this.assembly, this.selected!));
    this.select(null);
  }

  /** Waits for the target of `face` to be clicked in 3D. */
  startPick(face: Face) {
    this.pending = this.pending === face ? null : face;
  }

  /**
   * Holds the waiting face against the face clicked in 3D. A face can only sit
   * against one on the same axis, and not against a repeated Part.
   */
  onPick(picked: PickedFace) {
    const face = this.pending;
    if (!face || !this.selected) {
      return;
    }
    if (faceAxis(picked.face) !== faceAxis(face) || picked.to === this.selected
      || (picked.to !== CABINET && !this.targetsFor(this.selected).includes(picked.to))) {
      this.message = $localize`:@@designer.pickSameAxis:Choisissez une face de la même direction, sur le caisson ou une autre pièce non répétée.`;
      return;
    }
    const target = { to: picked.to, face: picked.face };
    const stack = this.selectedStack;
    if (stack) {
      this.setAssembly(updateStack(this.assembly, stack.id, attachStack(stack, face, target)));
    } else {
      const part = this.partOf(this.selected)!;
      this.setAssembly(updatePart(this.assembly, part.id, attach(part, face, target)));
    }
    this.pending = null;
    this.message = "";
  }

  updatePartIn(part: AssemblyPart): TemplateAssembly {
    return updatePart(this.assembly, this.selected!, part);
  }

  setDividerPart(assembly: TemplateAssembly, part: AssemblyPart): TemplateAssembly {
    return { ...assembly, divider: part };
  }

  updateStackIn(stack: FrontStack): TemplateAssembly {
    return updateStack(this.assembly, this.selected!, stack);
  }

  changeHardware(change: { index: number; item: Hardware }) {
    this.setAssembly(updateHardware(this.assembly, change.index, change.item));
  }

  addHardware() {
    this.setAssembly(addHardware(this.assembly));
  }

  removeHardware(index: number) {
    this.setAssembly(removeHardware(this.assembly, index));
  }

  save() {
    this.saving = true;
    this.error = "";
    this.templateService.update(this.template!.id, this.name, this.draft!)
      .pipe(tap(template => this.load(template)))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@designer.saveError:Impossible d'enregistrer le modèle.`) });
  }

  /** Saves, then publishes the draft as the next version. */
  publish() {
    this.saving = true;
    this.error = "";
    const note = this.changeNote.trim() || null;
    this.templateService.update(this.template!.id, this.name, this.draft!)
      .pipe(switchMap(() => this.templateService.publish(this.template!.id, note)))
      .pipe(tap(template => {
        this.load(template);
        this.publishing = false;
        this.changeNote = "";
        this.message = $localize`:@@designer.published:Version ${template.versions.length}:version: publiée.`;
        this.offerSwitch = template.versions.length === 1 && !!template.sourceCabinetId;
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.onPublishError(response) });
  }

  /** Back to the Project, with the template set on the Cabinet it was made from, for the Member to save. */
  switchCabinet() {
    this.router.navigate(["/projects", this.template!.projectId], {
      queryParams: { switchCabinet: this.template!.sourceCabinetId, template: this.template!.id, version: 1 },
    });
  }

  private onPublishError(response: HttpErrorResponse) {
    const body = response.error as ErrorResponse | null;
    if (response.status === 422 && body?.details) {
      this.violations = body.details as TemplateViolation[];
      this.error = $localize`:@@designer.rulesBroken:Le serveur a refusé le modèle : corrigez les règles ci-dessous.`;
    } else {
      this.error = this.errorOf(response, $localize`:@@designer.publishError:Impossible de publier le modèle.`);
    }
  }

  private errorOf(response: HttpErrorResponse, fallback: string): string {
    return (response.error as ErrorResponse | null)?.message ?? fallback;
  }
}
