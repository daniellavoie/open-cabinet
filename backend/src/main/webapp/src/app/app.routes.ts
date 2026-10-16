import { Routes } from "@angular/router";

import { CatalogComponent } from "./catalog/catalog/catalog.component";
import { ClientsComponent } from "./client/clients/clients.component";
import { LayoutComponent } from "./layout/layout.component";
import { memberGuardFn, ownerGuardFn } from "./member/member-guard";
import { OnboardingComponent } from "./onboarding/onboarding.component";
import { ProjectsComponent } from "./project/projects/projects.component";
import { SettingsComponent } from "./shop/settings/settings.component";

export const routes: Routes = [
  { path: "onboarding", component: OnboardingComponent },
  {
    path: "",
    component: LayoutComponent,
    canActivate: [memberGuardFn],
    children: [
      { path: "", pathMatch: "full", redirectTo: "projects" },
      { path: "projects", component: ProjectsComponent },
      {
        path: "projects/:projectId",
        loadComponent: () => import("./project/project-editor/project-editor.component").then(module => module.ProjectEditorComponent),
      },
      { path: "clients", component: ClientsComponent },
      { path: "settings", component: SettingsComponent, canActivate: [ownerGuardFn] },
      { path: "catalog", component: CatalogComponent, canActivate: [ownerGuardFn] },
    ],
  },
  { path: "**", redirectTo: "" },
];
