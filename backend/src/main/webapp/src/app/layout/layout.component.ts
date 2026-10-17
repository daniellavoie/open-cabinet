import { NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, Inject, LOCALE_ID } from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";

import { MemberService } from "../member/member-service";
import { AuthService } from "../security/auth-service";

@Component({
  selector: "layout",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgIf, RouterLink, RouterLinkActive, RouterOutlet],
  styleUrls: ["./layout.component.css"],
  templateUrl: "./layout.component.html",
})
export class LayoutComponent {
  /**
   * The other language, when the app is served per language (/fr/, /en/);
   * the dev server serves French only.
   */
  readonly otherLanguage: { code: string; label: string } | null;

  constructor(
    public memberService: MemberService,
    private authService: AuthService,
    @Inject(LOCALE_ID) locale: string,
  ) {
    const served = /^\/(fr|en)\/$/.test(new URL(document.baseURI).pathname);
    this.otherLanguage = !served ? null : locale.startsWith("fr")
      ? { code: "en", label: "English" }
      : { code: "fr", label: "Français" };
  }

  /**
   * Remembers the choice for the server (which sends visitors to their
   * language) and reloads the same page in the other language.
   */
  switchLanguage() {
    const code = this.otherLanguage!.code;
    document.cookie = `oc-locale=${code}; path=/; max-age=31536000; SameSite=Lax`;
    const base = new URL(document.baseURI).pathname;
    const route = window.location.pathname.substring(base.length - 1);
    window.location.assign(`/${code}${route}${window.location.search}`);
  }

  logout() {
    this.memberService.clear();
    this.authService.logout();
  }
}
