/// <reference types="@angular/localize" />

import { bootstrapApplication } from "@angular/platform-browser";

import { AppComponent } from "./app/app.component";
import { appConfig } from "./app/app.config";

bootstrapApplication(AppComponent, appConfig).catch(error => {
  console.error(error);
  showStartupFailure();
});

/**
 * Sign-in or startup failed (e.g. Keycloak unreachable): explain and offer a
 * retry rather than leaving a blank page.
 */
function showStartupFailure() {
  const panel = document.createElement("main");
  panel.className = "startup-failure";

  const title = document.createElement("h1");
  title.textContent = $localize`:@@startup.title:Open Cabinet n'a pas pu démarrer`;
  const message = document.createElement("p");
  message.textContent = $localize`:@@startup.message:La connexion a échoué. Vérifiez votre réseau, puis réessayez.`;
  const retry = document.createElement("button");
  retry.className = "btn btn-primary";
  retry.textContent = $localize`:@@startup.retry:Réessayer`;
  retry.addEventListener("click", () => window.location.assign("/"));

  panel.append(title, message, retry);
  document.body.replaceChildren(panel);
}
