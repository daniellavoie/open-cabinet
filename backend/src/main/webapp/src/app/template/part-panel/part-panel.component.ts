import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { Material } from "../../catalog/model/material";
import { PartRole } from "../../cutlist/model/project-output";
import { faceAxis } from "../../project/assembly";
import { AssemblyPart, Attachment, Axis, Face, Term } from "../../project/model/design";
import { AXIS_LABELS, FACE_LABELS, GRAIN_LABELS, PART_ROLE_LABELS, THICKNESS_LABELS } from "../../util/labels";
import { Units } from "../../util/units/units";
import { AttachmentEditorComponent } from "../attachment-editor/attachment-editor.component";
import {
  BANDING_ROLES, choices, choiceValue, fromChoice, MaterialChoices, MOULDING_ROLES, PANEL_ROLES,
} from "../material-choice";
import { attach, FACES } from "../template-edits";
import { TermsEditorComponent } from "../terms-editor/terms-editor.component";

/** Trim is added per Cabinet in a Project, not drawn in a template. */
const ROLES = (Object.keys(PART_ROLE_LABELS) as PartRole[])
  .filter(role => !["FILLER", "TOE_KICK", "FINISHED_END"].includes(role));

const AXES: Axis[] = ["X", "Y", "Z"];

/**
 * A Part's properties in the designer: what it is, its material, how it
 * stands and its grain, banding, repetition, and how each face is held.
 */
@Component({
  selector: "part-panel",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [AttachmentEditorComponent, FormsModule, NgFor, NgIf, TermsEditorComponent],
  styleUrls: ["./part-panel.component.css"],
  templateUrl: "./part-panel.component.html",
})
export class PartPanelComponent implements OnChanges {
  readonly roles = ROLES;
  readonly roleLabels = PART_ROLE_LABELS;
  readonly faceLabels = FACE_LABELS;
  readonly axisLabels = AXIS_LABELS;
  readonly thicknessLabels = THICKNESS_LABELS;
  readonly grainLabels = GRAIN_LABELS;
  readonly axes = AXES;
  readonly choiceValue = choiceValue;

  @Input() part!: AssemblyPart;
  /** Names of the Parts this one can be held against. */
  @Input() targets: string[] = [];
  @Input() materials: Material[] = [];
  @Input() units: Units = "IMPERIAL";
  /** The face whose target is being picked in 3D. */
  @Input() pendingFace: Face | null = null;
  /** A divider is placed across the Cabinet by each Cabinet's dividers. */
  @Input() divider = false;
  @Input() readOnly = false;

  @Output() partChange = new EventEmitter<AssemblyPart>();
  @Output() rename = new EventEmitter<string>();
  @Output() remove = new EventEmitter<void>();
  @Output() pick = new EventEmitter<Face>();

  name = "";
  panelChoices!: MaterialChoices;
  mouldingChoices!: MaterialChoices;
  bandingChoices!: MaterialChoices;
  thicknessChoices!: MaterialChoices;

  ngOnChanges(changes: SimpleChanges) {
    if (changes["part"]?.previousValue?.id !== this.part.id) {
      this.name = this.part.id;
    }
    if (!changes["materials"]) {
      return;
    }
    this.panelChoices = choices(PANEL_ROLES, ["SHEET"], this.materials);
    this.mouldingChoices = choices(MOULDING_ROLES, ["MOULDING"], this.materials);
    this.bandingChoices = choices(BANDING_ROLES, ["EDGE_BANDING"], this.materials);
    this.thicknessChoices = choices(PANEL_ROLES, ["SHEET", "FRONT"], this.materials);
  }

  get panel(): boolean {
    return (this.part.kind ?? "PANEL") === "PANEL";
  }

  /** Faces held by attachments: a divider's sides are placed by each Cabinet. */
  get faces(): Face[] {
    return this.divider ? FACES.filter(face => faceAxis(face) !== "X") : FACES;
  }

  /** The panel's faces its edges are on: not its two broad faces. */
  get edgeFaces(): Face[] {
    return FACES.filter(face => faceAxis(face) !== this.part.thickness);
  }

  /** Axes a size can be given on: not the thickness, which is the material's. */
  get sizeAxes(): Axis[] {
    return AXES.filter(axis => !(this.panel && axis === this.part.thickness) && !(this.divider && axis === "X"));
  }

  /** The panel's in-plane axes, where its grain can run. */
  get grainAxes(): Axis[] {
    return AXES.filter(axis => axis !== this.part.thickness);
  }

  commitName() {
    if (this.name.trim() && this.name.trim() !== this.part.id) {
      this.rename.emit(this.name.trim());
    } else {
      this.name = this.part.id;
    }
  }

  set(changes: Partial<AssemblyPart>) {
    this.partChange.emit({ ...this.part, ...changes });
  }

  setMaterial(value: string) {
    this.set({ material: fromChoice(value) });
  }

  setKind(kind: "PANEL" | "MOULDING") {
    this.set(kind === "MOULDING"
      ? { kind, material: { role: "CROWN_MOULDING" }, thickness: null, repeat: null, banding: null, front: null }
      : { kind, material: { role: "CARCASS" }, thickness: "Y" });
  }

  setThickness(thickness: Axis) {
    this.set({ thickness, grain: this.part.grain === thickness ? null : this.part.grain });
  }

  setRepeated(repeated: boolean) {
    this.set({ repeat: repeated ? { count: "SHELVES", dividerGapMm: 1 } : null });
  }

  banded(face: Face): boolean {
    return this.part.banding?.faces.includes(face) ?? false;
  }

  setBanded(face: Face, banded: boolean) {
    const faces = (this.part.banding?.faces ?? []).filter(current => current !== face);
    this.set({
      banding: { faces: banded ? [...faces, face] : faces, material: this.part.banding?.material ?? { role: "CARCASS_BANDING" } },
    });
  }

  setBandingMaterial(value: string) {
    this.set({ banding: { faces: this.part.banding?.faces ?? [], material: fromChoice(value) } });
  }

  setAttachment(face: Face, attachment: Attachment | null) {
    this.partChange.emit(attach(this.part, face, attachment));
  }

  sized(axis: Axis): boolean {
    return !!this.part.sizes?.[axis];
  }

  setSized(axis: Axis, sized: boolean) {
    const sizes = { ...this.part.sizes ?? {} };
    if (sized) {
      sizes[axis] = [{ mm: 101.6 }];
    } else {
      delete sizes[axis];
    }
    this.set({ sizes });
  }

  setSize(axis: Axis, terms: Term[]) {
    this.set({ sizes: { ...this.part.sizes ?? {}, [axis]: terms } });
  }
}
