import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable, signal } from "@angular/core";
import { catchError, map, Observable, of, switchMap, tap, throwError } from "rxjs";

import { Shop } from "../shop/model/shop";
import { Member } from "./model/member";

/**
 * The signed-in Member and their Shop. Both come from the Open Cabinet API,
 * never from the Keycloak token (docs/adr/0003-keycloak-for-identity-only.md).
 */
@Injectable({ providedIn: "root" })
export class MemberService {
  readonly member = signal<Member | null>(null);
  readonly shop = signal<Shop | null>(null);

  constructor(private httpClient: HttpClient) {}

  /**
   * Loads the Member and Shop; emits false when this person has not joined a Shop yet.
   */
  load(): Observable<boolean> {
    if (this.member() && this.shop()) {
      return of(true);
    }

    return this.httpClient.get<Member>("/api/member/me")
      .pipe(switchMap(member => this.httpClient.get<Shop>("/api/shop")
        .pipe(tap(shop => {
          this.member.set(member);
          this.shop.set(shop);
        }))))
      .pipe(map(() => true))
      .pipe(catchError((error: HttpErrorResponse) => error.status === 404 ? of(false) : throwError(() => error)));
  }

  isOwner(): boolean {
    return this.member()?.role === "OWNER";
  }

  setShop(shop: Shop) {
    this.shop.set(shop);
  }

  clear() {
    this.member.set(null);
    this.shop.set(null);
  }
}
