import { provideHttpClient, withInterceptors } from "@angular/common/http";
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
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
  ],
};
