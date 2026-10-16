import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { switchMap } from "rxjs";

import { AuthService } from "./auth-service";

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith("/api/")) {
    return next(request);
  }

  const authService = inject(AuthService);

  return authService.token()
    .pipe(switchMap(token => next(token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request)));
};
