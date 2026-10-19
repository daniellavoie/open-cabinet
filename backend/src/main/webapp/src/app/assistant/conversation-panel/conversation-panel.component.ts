import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import {
  ChangeDetectionStrategy, Component, EventEmitter, Inject, Input, LOCALE_ID, OnChanges, OnDestroy, Output, SimpleChanges,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, from, Subscription, switchMap, takeWhile, tap, timer } from "rxjs";

import { Project } from "../../project/model/project";
import { ANSWER_ERROR_LABELS, ASSISTANT_TOOL_LABELS, remarkLabel, VIOLATION_LABELS } from "../../util/labels";
import { Units } from "../../util/units/units";
import { ErrorResponse } from "../../util/model/error-response";
import { AssistantService } from "../assistant-service";
import { AssistantStatus } from "../model/assistant";
import { ACCEPTED_TYPES, filesProblem, prepare, sizeLabel } from "../attachment-files";
import { AcceptResult, Attachment, ConversationView, DesignProposal, ProgressEvent, remarkKey } from "../model/conversation";

const POLL_MS = 1000;

/**
 * A Project's Conversation with the Design Assistant and its Design Proposal
 * (ADR 0004). Chatting needs no editing lock; accepting replays the Proposal
 * onto the saved design under the lock, so the editor must have no unsaved
 * changes.
 */
@Component({
  selector: "conversation-panel",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, NgFor, NgIf],
  styleUrls: ["./conversation-panel.component.css"],
  templateUrl: "./conversation-panel.component.html",
})
export class ConversationPanelComponent implements OnChanges, OnDestroy {
  @Input() projectId!: string;
  /** The editor has unsaved changes. */
  @Input() dirty = false;
  /** Someone else is editing the Project. */
  @Input() readOnly = false;
  @Input() units: Units = "IMPERIAL";

  @Output() proposalChange = new EventEmitter<DesignProposal | null>();
  /** The Project saved by accepting or undoing. */
  @Output() projectChange = new EventEmitter<Project>();

  readonly errorLabels = ANSWER_ERROR_LABELS;
  readonly violationLabels = VIOLATION_LABELS;

  status?: AssistantStatus;
  view?: ConversationView;
  progress: ProgressEvent[] = [];
  /** The Proposal's remarks as Members read them, with the assistant's explanation when it kept one. */
  remarkLines: { text: string; explanation: string | null }[] = [];
  text = "";
  /** Files chosen for the next message. */
  files: File[] = [];
  filesError = "";
  /** Image attachments shown as thumbnails, by attachment id. */
  thumbnails = new Map<string, string>();
  readonly acceptedTypes = ACCEPTED_TYPES.join(",");
  sending = false;
  acting = false;
  outcome = "";
  error = "";
  private polling?: Subscription;

  constructor(
    private assistantService: AssistantService,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    assistantService.status().pipe(tap(status => this.status = status)).subscribe();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes["projectId"]) {
      this.load();
    }
  }

  ngOnDestroy() {
    this.polling?.unsubscribe();
    this.thumbnails.forEach(url => URL.revokeObjectURL(url));
  }

  choose(input: HTMLInputElement) {
    this.files = [...this.files, ...Array.from(input.files ?? [])];
    input.value = "";
    this.filesError = filesProblem(this.files) ?? "";
  }

  removeFile(index: number) {
    this.files = this.files.filter((_, at) => at !== index);
    this.filesError = filesProblem(this.files) ?? "";
  }

  size(bytes: number): string {
    return sizeLabel(bytes, this.locale);
  }

  isImage(attachment: Attachment): boolean {
    return attachment.mediaType.startsWith("image/");
  }

  /** Opens a PDF, or a photo full size, in a new tab. */
  open(attachment: Attachment) {
    this.assistantService.attachment(this.projectId, attachment.id)
      .pipe(tap(blob => {
        const url = URL.createObjectURL(blob);
        window.open(url, "_blank", "noopener");
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      }))
      .subscribe();
  }

  get proposal(): DesignProposal | null {
    return this.view?.proposal ?? null;
  }

  get canSend(): boolean {
    return !!this.status?.enabled && !this.view?.running && !this.sending && !!this.text.trim() && !this.filesError;
  }

  /** Why the open Proposal cannot be accepted now, if it cannot. */
  get acceptBlocker(): string | null {
    if (this.readOnly) {
      return $localize`:@@conversation.blockedLock:Quelqu'un d'autre modifie le projet.`;
    }
    if (this.dirty) {
      return $localize`:@@conversation.blockedDirty:Enregistrez ou annulez vos modifications d'abord.`;
    }
    if (this.view?.running) {
      return $localize`:@@conversation.blockedRunning:L'assistant travaille encore sur la proposition.`;
    }
    return null;
  }

  describe(event: ProgressEvent): string {
    const tool = event.tool ? ASSISTANT_TOOL_LABELS[event.tool] ?? event.tool : "";
    switch (event.kind) {
      case "THINKING":
        return $localize`:@@conversation.thinking:Réflexion…`;
      case "STEP":
        return tool;
      case "NOTED":
        return tool;
      case "REFUSED":
        return $localize`:@@conversation.refused:Refusé : ${tool}:tool:`;
    }
  }

  reasons(event: ProgressEvent): string[] {
    return [
      ...(event.violations ?? []).map(violation => this.violationLabels[violation.code] ?? violation.code),
      ...(event.problem ? [event.problem] : []),
    ];
  }

  send() {
    if (!this.canSend) {
      return;
    }
    this.sending = true;
    this.error = "";
    this.outcome = "";
    const language = this.locale.startsWith("en") ? "en" : "fr";
    from(Promise.all(this.files.map(prepare)))
      .pipe(switchMap(files => this.assistantService.send(this.projectId, this.text.trim(), language, files)))
      .pipe(tap(view => {
        this.text = "";
        this.files = [];
        this.progress = [];
        this.show(view);
      }))
      .pipe(finalize(() => this.sending = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@conversation.sendError:Impossible d'envoyer le message.`) });
  }

  accept() {
    this.act(this.assistantService.accept(this.projectId, this.proposal!.id), result => {
      this.outcome = result.appliedSteps === result.totalSteps
        ? $localize`:@@conversation.accepted:Proposition acceptée et projet enregistré.`
        : $localize`:@@conversation.partlyAccepted:${result.appliedSteps}:applied: étape(s) sur ${result.totalSteps}:total: appliquée(s) : la suivante ne respecterait plus les règles du projet tel qu'il est maintenant.`;
    });
  }

  undo() {
    this.act(this.assistantService.undoAccept(this.projectId, this.proposal!.id), () => {
      this.outcome = $localize`:@@conversation.undone:Acceptation annulée : le projet est revenu à son état d'avant.`;
    });
  }

  discard() {
    this.acting = true;
    this.error = "";
    this.assistantService.discard(this.projectId, this.proposal!.id)
      .pipe(tap(() => {
        this.outcome = "";
        this.load();
      }))
      .pipe(finalize(() => this.acting = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@conversation.discardError:Impossible de rejeter la proposition.`) });
  }

  private act(request: ReturnType<AssistantService["accept"]>, done: (result: AcceptResult) => void) {
    this.acting = true;
    this.error = "";
    request
      .pipe(tap(result => {
        done(result);
        this.projectChange.emit(result.project);
        this.load();
      }))
      .pipe(finalize(() => this.acting = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@conversation.acceptError:Impossible de modifier le projet.`) });
  }

  private load() {
    this.polling?.unsubscribe();
    this.assistantService.conversation(this.projectId)
      .pipe(tap(view => {
        this.progress = [];
        this.show(view);
      }))
      .subscribe();
  }

  /** Shows the Conversation, and polls it while the assistant works. */
  private show(view: ConversationView) {
    const proposalBefore = JSON.stringify(this.view?.proposal ?? null);
    this.view = view;
    this.progress = [...this.progress, ...view.progress];
    this.remarkLines = this.linesOf(view.proposal);
    this.loadThumbnails(view);
    if (JSON.stringify(view.proposal) !== proposalBefore) {
      this.proposalChange.emit(view.proposal);
    }
    if (view.running && !this.polling) {
      this.polling = timer(POLL_MS, POLL_MS)
        .pipe(switchMap(() => this.assistantService.conversation(this.projectId, this.lastSequence())))
        .pipe(takeWhile(next => next.running, true))
        .pipe(finalize(() => this.polling = undefined))
        .subscribe(next => this.show(next));
    }
  }

  private linesOf(proposal: DesignProposal | null): { text: string; explanation: string | null }[] {
    if (!proposal) {
      return [];
    }
    const walls = proposal.design.walls.map(wall => wall.id);
    return (proposal.remarks ?? []).map(remark => {
      const index = remark.wallId ? walls.indexOf(remark.wallId) : -1;
      return {
        text: remarkLabel(remark, index >= 0 ? index + 1 : null, this.units),
        explanation: proposal.remarkExplanations?.[remarkKey(remark)] ?? null,
      };
    });
  }

  private loadThumbnails(view: ConversationView) {
    view.messages.flatMap(message => message.attachments ?? [])
      .filter(attachment => this.isImage(attachment) && !this.thumbnails.has(attachment.id))
      .forEach(attachment => {
        this.thumbnails.set(attachment.id, "");
        this.assistantService.attachment(this.projectId, attachment.id)
          .pipe(tap(blob => this.thumbnails.set(attachment.id, URL.createObjectURL(blob))))
          .subscribe();
      });
  }

  private lastSequence(): number {
    return this.progress.length > 0 ? this.progress[this.progress.length - 1].sequence : 0;
  }

  private errorOf(response: HttpErrorResponse, fallback: string): string {
    return (response.error as ErrorResponse | null)?.message ?? fallback;
  }
}
