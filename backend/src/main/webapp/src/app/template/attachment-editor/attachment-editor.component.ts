import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { CABINET, faceAxis, faceOf } from "../../project/assembly";
import { Attachment, Face, Term } from "../../project/model/design";
import { FACE_LABELS } from "../../util/labels";
import { Units } from "../../util/units/units";
import { MaterialChoices } from "../material-choice";
import { TermsEditorComponent } from "../terms-editor/terms-editor.component";

/**
 * One face of a Part or front stack: free, or held against a face of the
 * Cabinet or another Part (on the same axis), with an offset. "Pick in 3D"
 * lets the Member click the target instead.
 */
@Component({
  selector: "attachment-editor",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, TermsEditorComponent],
  styleUrls: ["./attachment-editor.component.css"],
  templateUrl: "./attachment-editor.component.html",
})
export class AttachmentEditorComponent {
  readonly faceLabels = FACE_LABELS;
  readonly cabinet = CABINET;

  @Input() face!: Face;
  @Input() attachment: Attachment | null = null;
  /** Parts this face can be held against. */
  @Input() targets: string[] = [];
  @Input() units: Units = "IMPERIAL";
  @Input() thicknesses: MaterialChoices = { roles: [], catalog: [] };
  /** Whether the face may be left free. */
  @Input() optional = true;
  /** Whether the Member is picking this face's target in 3D. */
  @Input() picking = false;
  @Input() readOnly = false;

  @Output() attachmentChange = new EventEmitter<Attachment | null>();
  @Output() pick = new EventEmitter<void>();

  /** The target's two faces on this face's axis, opposite first: a side sits against another's far face. */
  get targetFaces(): Face[] {
    const axis = faceAxis(this.face);
    const sameEnd = this.face;
    const other = faceOf(axis, this.face === faceOf(axis, false));
    return this.attachment?.to === CABINET ? [sameEnd, other] : [other, sameEnd];
  }

  setTarget(to: string) {
    if (!to) {
      this.attachmentChange.emit(null);
      return;
    }
    const axis = faceAxis(this.face);
    const face = to === CABINET ? this.face : faceOf(axis, this.face === faceOf(axis, false));
    this.attachmentChange.emit({ to, face, offset: this.attachment?.offset ?? null });
  }

  setFace(face: Face) {
    this.attachmentChange.emit({ ...this.attachment!, face });
  }

  setOffset(offset: Term[]) {
    this.attachmentChange.emit({ ...this.attachment!, offset: offset.length > 0 ? offset : null });
  }
}
