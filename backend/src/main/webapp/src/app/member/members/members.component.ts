import { DatePipe, NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, forkJoin, Observable, tap } from "rxjs";

import { InvitationService } from "../../invitation/invitation-service";
import { Invitation } from "../../invitation/model/invitation";
import { ROLE_LABELS } from "../../util/labels";
import { MemberService } from "../member-service";
import { Member, Role } from "../model/member";

/**
 * Owners manage who belongs to the Shop: roles, removal and Invitations.
 */
@Component({
  selector: "members",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [DatePipe, FormsModule, NgFor, NgIf],
  styleUrls: ["./members.component.css"],
  templateUrl: "./members.component.html",
})
export class MembersComponent {
  readonly roles = Object.entries(ROLE_LABELS) as [Role, string][];
  readonly roleLabels = ROLE_LABELS;

  members: Member[] = [];
  invitations: Invitation[] = [];
  email = "";
  role: Role = "DESIGNER";
  link = "";
  copied = false;
  busy = false;
  error = "";

  constructor(
    public memberService: MemberService,
    private invitationService: InvitationService,
  ) {
    this.load();
  }

  load() {
    forkJoin([this.memberService.findAll(), this.invitationService.findPending()])
      .pipe(tap(([members, invitations]) => {
        this.members = members;
        this.invitations = invitations;
      }))
      .subscribe();
  }

  invite() {
    this.copied = false;
    this.run(this.invitationService.create(this.email || null, this.role), created => {
      this.link = this.invitationService.link(created.token);
      this.email = "";
    });
  }

  copy() {
    navigator.clipboard?.writeText(this.link).then(() => this.copied = true);
  }

  revoke(invitation: Invitation) {
    this.run(this.invitationService.revoke(invitation.id), () => undefined);
  }

  changeRole(member: Member, role: Role) {
    this.run(this.memberService.updateRole(member.id, role), () => undefined);
  }

  remove(member: Member) {
    this.run(this.memberService.remove(member.id), () => undefined);
  }

  private run<T>(request: Observable<T>, then: (result: T) => void) {
    this.busy = true;
    this.error = "";
    request
      .pipe(tap(result => then(result)))
      .pipe(finalize(() => {
        this.busy = false;
        this.load();
      }))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@members.error:L'opération a échoué.` });
  }
}
