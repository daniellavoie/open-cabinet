import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { Member, Role } from "../member/model/member";
import { CreatedInvitation, Invitation, InvitationPreview } from "./model/invitation";

@Injectable({ providedIn: "root" })
export class InvitationService {
  constructor(private httpClient: HttpClient) {}

  create(email: string | null, role: Role): Observable<CreatedInvitation> {
    return this.httpClient.post<CreatedInvitation>("/api/invitation", { email, role });
  }

  findPending(): Observable<Invitation[]> {
    return this.httpClient.get<Invitation[]>("/api/invitation");
  }

  revoke(invitationId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/invitation/${invitationId}`);
  }

  preview(token: string): Observable<InvitationPreview> {
    return this.httpClient.get<InvitationPreview>(`/api/invitation/token/${encodeURIComponent(token)}`);
  }

  accept(token: string, memberName: string, memberEmail: string | null): Observable<Member> {
    return this.httpClient.post<Member>(`/api/invitation/token/${encodeURIComponent(token)}/accept`, { memberName, memberEmail });
  }

  /**
   * The link an Owner passes on. Only shown once: the server keeps a hash of the token.
   */
  link(token: string): string {
    return `${window.location.origin}${document.baseURI.replace(window.location.origin, "").replace(/\/$/, "")}/invite/${token}`;
  }
}
