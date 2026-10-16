import { Injectable, NgZone } from "@angular/core";
import Keycloak from "keycloak-js";
import { Observable, of } from "rxjs";

import { SecurityConfig } from "./model/security-config";

/**
 * Signs the Member in through Keycloak. Keycloak is identity only
 * (docs/adr/0003-keycloak-for-identity-only.md): the Member's Shop and role
 * come from the Open Cabinet API, never from token claims.
 */
@Injectable({ providedIn: "root" })
export class AuthService {
  private keycloak?: Keycloak;

  constructor(private ngZone: NgZone) {}

  async init(): Promise<void> {
    const response = await fetch("/api/security/config");
    const config: SecurityConfig = await response.json();

    const issuer = new URL(config.issuerUri);
    const realmMarker = "/realms/";
    const realmIndex = issuer.pathname.lastIndexOf(realmMarker);

    this.keycloak = new Keycloak({
      url: issuer.origin + issuer.pathname.substring(0, realmIndex),
      realm: issuer.pathname.substring(realmIndex + realmMarker.length),
      clientId: config.clientId,
    });

    await this.keycloak.init({
      onLoad: "login-required",
      pkceMethod: "S256",
      checkLoginIframe: false,
    });
  }

  /**
   * A valid access token, refreshed first when it expires within 30 seconds.
   * keycloak-js resolves its promises outside Angular's zone, so the result is
   * handed back inside the zone; otherwise the request it authorizes, and the
   * view updates that follow, would run unseen by change detection.
   */
  token(): Observable<string | undefined> {
    const keycloak = this.keycloak;
    if (!keycloak || !keycloak.isTokenExpired(30)) {
      return of(keycloak?.token);
    }

    return new Observable(subscriber => {
      keycloak.updateToken(30)
        .then(() => this.ngZone.run(() => {
          subscriber.next(keycloak.token);
          subscriber.complete();
        }))
        .catch(error => this.ngZone.run(() => subscriber.error(error)));
    });
  }

  displayName(): string {
    const profile = this.keycloak?.tokenParsed as { name?: string; preferred_username?: string } | undefined;
    return profile?.name ?? profile?.preferred_username ?? "";
  }

  email(): string | null {
    const profile = this.keycloak?.tokenParsed as { email?: string } | undefined;
    return profile?.email ?? null;
  }

  logout(): Promise<void> {
    return this.keycloak?.logout({ redirectUri: window.location.origin + "/" }) ?? Promise.resolve();
  }
}
