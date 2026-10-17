import { provideHttpClient, withInterceptors, withXhr } from "@angular/common/http";
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from "@angular/core";
import { provideRouter } from "@angular/router";

import { routes } from "./app.routes";
import { authInterceptor } from "./security/auth-interceptor";
import { AuthService } from "./security/auth-service";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAppInitializer(() => inject(AuthService).init()),
    // XHR rather than Angular 22's default fetch backend: zone.js tracks XHR
    // callbacks, so responses handled in subscribe() trigger change detection.
    provideHttpClient(withXhr(), withInterceptors([authInterceptor])),
    provideRouter(routes),
  ],
};
