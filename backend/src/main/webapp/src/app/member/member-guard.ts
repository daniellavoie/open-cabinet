import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { map } from "rxjs";

import { MemberService } from "./member-service";

/**
 * Sends people who have not joined a Shop to onboarding.
 */
export const memberGuardFn: CanActivateFn = () => {
  const router = inject(Router);

  return inject(MemberService).load()
    .pipe(map(isMember => isMember ? true : router.parseUrl("/onboarding")));
};

/**
 * Owner-only screens are hidden from Designers; the server still enforces the role.
 */
export const ownerGuardFn: CanActivateFn = () => {
  const router = inject(Router);
  const memberService = inject(MemberService);

  return memberService.load()
    .pipe(map(isMember => isMember && memberService.isOwner() ? true : router.parseUrl("/projects")));
};
