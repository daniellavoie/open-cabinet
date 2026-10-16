import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { Quote, QuoteStatus } from "./model/quote";

@Injectable({ providedIn: "root" })
export class QuoteService {
  constructor(private httpClient: HttpClient) {}

  findByProject(projectId: string): Observable<Quote[]> {
    return this.httpClient.get<Quote[]>(`/api/project/${projectId}/quote`);
  }

  create(projectId: string): Observable<Quote> {
    return this.httpClient.post<Quote>(`/api/project/${projectId}/quote`, {});
  }

  send(quoteId: string): Observable<Quote> {
    return this.httpClient.post<Quote>(`/api/quote/${quoteId}/send`, {});
  }

  updateStatus(quoteId: string, status: QuoteStatus): Observable<Quote> {
    return this.httpClient.put<Quote>(`/api/quote/${quoteId}/status`, { status });
  }

  delete(quoteId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/quote/${quoteId}`);
  }

  pdf(quoteId: string): Observable<Blob> {
    return this.httpClient.get(`/api/quote/${quoteId}/pdf`, { responseType: "blob" });
  }
}
