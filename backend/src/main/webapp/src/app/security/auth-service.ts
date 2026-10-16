import { Injectable } from "@angular/core";
import Keycloak from "keycloak-js";

import { SecurityConfig } from "./model/security-config";

/**
 * Signs the Member in through Keycloak. Keycloak is identity only
 * (docs/adr/0003-keycloak-for-identity-only.md): the Member's Shop and role
 * come from the Open Cabinet API, never from token claims.
 */
@Injectable({ providedIn: "root" })
export class AuthService {
  private keycloak?: Keycloak;

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

  async token(): Promise<string | undefined> {
    await this.keycloak?.updateToken(30);
    return this.keycloak?.token;
  }

  displayName(): string {
    const profile = this.keycloak?.tokenParsed as { name?: string; preferred_username?: string } | undefined;
    return profile?.name ?? profile?.preferred_username ?? "";
  }

  logout(): Promise<void> {
    return this.keycloak?.logout({ redirectUri: window.location.origin }) ?? Promise.resolve();
  }
}
