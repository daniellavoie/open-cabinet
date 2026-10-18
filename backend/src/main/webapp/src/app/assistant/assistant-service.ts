import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { AssistantStatus, CheckReply } from "./model/assistant";

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
}
