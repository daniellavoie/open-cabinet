import { NgIf } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { finalize, switchMap, tap } from "rxjs";

import { MemberService } from "../member/member-service";
import { AuthService } from "../security/auth-service";
import { ShopService } from "../shop/shop-service";

/**
 * First sign-in: the person creates their Shop and becomes its Owner.
 */
@Component({
  selector: "onboarding",
  standalone: true,
  imports: [FormsModule, NgIf],
  styleUrls: ["./onboarding.component.css"],
  templateUrl: "./onboarding.component.html",
})
export class OnboardingComponent implements OnInit {
  shopName = "";
  memberName = "";
  saving = false;
  error = "";

  constructor(
    private authService: AuthService,
    private memberService: MemberService,
    private shopService: ShopService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.memberService.load()
      .pipe(tap(isMember => {
        if (isMember) {
          this.router.navigateByUrl("/projects");
        }
      }))
      .subscribe();
    this.memberName = this.authService.displayName();
  }

  create() {
    this.saving = true;
    this.error = "";
    this.shopService.create({
      shopName: this.shopName,
      memberName: this.memberName,
      memberEmail: this.authService.email(),
    })
      .pipe(switchMap(() => this.memberService.load()))
      .pipe(tap(() => this.router.navigateByUrl("/projects")))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: () => this.error = $localize`:@@onboarding.error:Impossible de créer l'atelier.` });
  }
}
