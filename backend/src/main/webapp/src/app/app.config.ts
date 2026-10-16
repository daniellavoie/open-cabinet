import { provideHttpClient, withInterceptors, withXhr } from "@angular/common/http";
import { registerLocaleData } from "@angular/common";
import localeFrCa from "@angular/common/locales/fr-CA";
import { ApplicationConfig, inject, LOCALE_ID, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from "@angular/core";
import { provideRouter } from "@angular/router";

import { routes } from "./app.routes";
import { authInterceptor } from "./security/auth-interceptor";
import { AuthService } from "./security/auth-service";

registerLocaleData(localeFrCa);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: "fr-CA" },
    provideAppInitializer(() => inject(AuthService).init()),
    // XHR rather than Angular 22's default fetch backend: zone.js tracks XHR
    // callbacks, so responses handled in subscribe() trigger change detection.
    provideHttpClient(withXhr(), withInterceptors([authInterceptor])),
    provideRouter(routes),
  ],
};
