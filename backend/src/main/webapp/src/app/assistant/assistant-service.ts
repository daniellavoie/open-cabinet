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
  update(enabled: boolean, monthlyBudgetUsd: number): Observable<AssistantStatus> {
    return this.httpClient.put<AssistantStatus>("/api/assistant", { enabled, monthlyBudgetUsd });
  }

  /** A short test request, counted against the budget. Owners only. */
  check(): Observable<CheckReply> {
    return this.httpClient.post<CheckReply>("/api/assistant/check", {});
  }

  /** The Project's Conversation, with the progress events after `afterProgress`. */
  conversation(projectId: string, afterProgress = 0): Observable<ConversationView> {
    return this.httpClient.get<ConversationView>(`/api/project/${projectId}/conversation?afterProgress=${afterProgress}`);
  }

  /** Sends a message; the assistant answers in the background. */
  send(projectId: string, text: string, language: string): Observable<ConversationView> {
    return this.httpClient.post<ConversationView>(`/api/project/${projectId}/conversation/message`, { text, language });
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
