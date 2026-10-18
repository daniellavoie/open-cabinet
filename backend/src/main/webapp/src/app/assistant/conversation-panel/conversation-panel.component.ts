import { NgFor, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import {
  ChangeDetectionStrategy, Component, EventEmitter, Inject, Input, LOCALE_ID, OnChanges, OnDestroy, Output, SimpleChanges,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, Subscription, switchMap, takeWhile, tap, timer } from "rxjs";

import { Project } from "../../project/model/project";
import { ANSWER_ERROR_LABELS, ASSISTANT_TOOL_LABELS, VIOLATION_LABELS } from "../../util/labels";
import { ErrorResponse } from "../../util/model/error-response";
import { AssistantService } from "../assistant-service";
import { AssistantStatus } from "../model/assistant";
import { AcceptResult, ConversationView, DesignProposal, ProgressEvent } from "../model/conversation";

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

  @Output() proposalChange = new EventEmitter<DesignProposal | null>();
  /** The Project saved by accepting or undoing. */
  @Output() projectChange = new EventEmitter<Project>();

  readonly errorLabels = ANSWER_ERROR_LABELS;
  readonly violationLabels = VIOLATION_LABELS;

  status?: AssistantStatus;
  view?: ConversationView;
  progress: ProgressEvent[] = [];
  text = "";
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
  }

  get proposal(): DesignProposal | null {
    return this.view?.proposal ?? null;
  }

  get canSend(): boolean {
    return !!this.status?.enabled && !this.view?.running && !this.sending && !!this.text.trim();
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
    this.assistantService.send(this.projectId, this.text.trim(), language)
      .pipe(tap(view => {
        this.text = "";
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

  private lastSequence(): number {
    return this.progress.length > 0 ? this.progress[this.progress.length - 1].sequence : 0;
  }

  private errorOf(response: HttpErrorResponse, fallback: string): string {
    return (response.error as ErrorResponse | null)?.message ?? fallback;
  }
}
