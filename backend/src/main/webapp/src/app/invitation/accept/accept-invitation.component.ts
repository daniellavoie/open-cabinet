import { NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { finalize, switchMap, tap } from "rxjs";

import { MemberService } from "../../member/member-service";
import { AuthService } from "../../security/auth-service";
import { ROLE_LABELS } from "../../util/labels";
import { InvitationService } from "../invitation-service";
import { InvitationPreview } from "../model/invitation";

/**
 * Where an invitation link lands: the signed-in person joins the Shop.
 */
@Component({
  selector: "accept-invitation",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, NgIf, RouterLink],
  styleUrls: ["./accept-invitation.component.css"],
  templateUrl: "./accept-invitation.component.html",
})
export class AcceptInvitationComponent {
  readonly roleLabels = ROLE_LABELS;

  token: string;
  preview?: InvitationPreview;
  alreadyMember = false;
  invalid = false;
  memberName = "";
  saving = false;
  error = "";

  constructor(
    route: ActivatedRoute,
    private authService: AuthService,
    private memberService: MemberService,
    private invitationService: InvitationService,
    private router: Router,
  ) {
    this.token = route.snapshot.paramMap.get("token")!;
    this.memberName = this.authService.displayName();

    this.memberService.load()
      .pipe(tap(isMember => this.alreadyMember = isMember))
      .subscribe();
    this.invitationService.preview(this.token)
      .pipe(tap(preview => this.preview = preview))
      .subscribe({ error: () => this.invalid = true });
  }

  accept() {
    this.saving = true;
    this.error = "";
    this.invitationService.accept(this.token, this.memberName, this.authService.email())
      .pipe(switchMap(() => this.memberService.load()))
      .pipe(tap(() => this.router.navigateByUrl("/projects")))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@invite.error:Impossible d'accepter l'invitation.` });
  }
}
