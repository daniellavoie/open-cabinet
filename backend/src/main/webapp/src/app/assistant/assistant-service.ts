import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { AssistantStatus, CheckReply } from "./model/assistant";
import { AcceptResult, ConversationView, DesignProposal } from "./model/conversation";

@Injectable({ providedIn: "root" })
export class AssistantService {
  constructor(private httpClient: HttpClient) {}

  status(): Observable<AssistantStatus> {
    return this.httpClient.get<AssistantStatus>("/api/assistant");
  }

  /** Opts the Shop in or out and sets its monthly budget. Owners only. */
  update(enabled: boolean, monthlyBudgetUsd: number, styleText: string): Observable<AssistantStatus> {
    return this.httpClient.put<AssistantStatus>("/api/assistant", { enabled, monthlyBudgetUsd, styleText });
  }

  /** A short test request, counted against the budget. Owners only. */
  check(): Observable<CheckReply> {
    return this.httpClient.post<CheckReply>("/api/assistant/check", {});
  }

  /** The Project's Conversation, with the progress events after `afterProgress`. */
  conversation(projectId: string, afterProgress = 0): Observable<ConversationView> {
    return this.httpClient.get<ConversationView>(`/api/project/${projectId}/conversation?afterProgress=${afterProgress}`);
  }

  /** Sends a message, with its attachments; the assistant answers in the background. */
  send(projectId: string, text: string, language: string, files: File[] = []): Observable<ConversationView> {
    const url = `/api/project/${projectId}/conversation/message`;
    if (files.length === 0) {
      return this.httpClient.post<ConversationView>(url, { text, language });
    }
    const form = new FormData();
    form.append("text", text);
    form.append("language", language);
    files.forEach(file => form.append("files", file, file.name));
    return this.httpClient.post<ConversationView>(url, form);
  }

  /** An attachment's content, fetched with the Member's token. */
  attachment(projectId: string, attachmentId: string): Observable<Blob> {
    return this.httpClient.get(`/api/project/${projectId}/conversation/attachment/${attachmentId}`, { responseType: "blob" });
  }

  /** Replays the Proposal onto the Project's current design and saves it, under the editing lock. */
  accept(projectId: string, proposalId: string): Observable<AcceptResult> {
    return this.httpClient.post<AcceptResult>(`/api/project/${projectId}/proposal/${proposalId}/accept`, {});
  }

  /** Restores the design from before accepting, if nobody has saved the Project since. */
  undoAccept(projectId: string, proposalId: string): Observable<AcceptResult> {
    return this.httpClient.post<AcceptResult>(`/api/project/${projectId}/proposal/${proposalId}/undo`, {});
  }

  discard(projectId: string, proposalId: string): Observable<DesignProposal> {
    return this.httpClient.post<DesignProposal>(`/api/project/${projectId}/proposal/${proposalId}/discard`, {});
  }
}
