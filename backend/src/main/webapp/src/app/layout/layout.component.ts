import { NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
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
  constructor(
    public memberService: MemberService,
    private authService: AuthService,
  ) {}

  logout() {
    this.memberService.clear();
    this.authService.logout();
  }
}
