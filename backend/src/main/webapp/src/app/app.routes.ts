import { Routes } from "@angular/router";

import { CatalogComponent } from "./catalog/catalog/catalog.component";
import { AcceptInvitationComponent } from "./invitation/accept/accept-invitation.component";
import { ClientsComponent } from "./client/clients/clients.component";
import { LayoutComponent } from "./layout/layout.component";
import { memberGuardFn, ownerGuardFn } from "./member/member-guard";
import { MembersComponent } from "./member/members/members.component";
import { OnboardingComponent } from "./onboarding/onboarding.component";
import { ProjectsComponent } from "./project/projects/projects.component";
import { SettingsComponent } from "./shop/settings/settings.component";

export const routes: Routes = [
  { path: "onboarding", component: OnboardingComponent },
  { path: "invite/:token", component: AcceptInvitationComponent },
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
      {
        path: "projects/:projectId/templates/:templateId",
        loadComponent: () => import("./template/template-designer/template-designer.component").then(module => module.TemplateDesignerComponent),
      },
      {
        path: "templates",
        loadComponent: () => import("./template/template-library/template-library.component").then(module => module.TemplateLibraryComponent),
      },
      {
        path: "templates/:templateId",
        loadComponent: () => import("./template/template-designer/template-designer.component").then(module => module.TemplateDesignerComponent),
      },
      { path: "clients", component: ClientsComponent },
      { path: "settings", component: SettingsComponent, canActivate: [ownerGuardFn] },
      { path: "catalog", component: CatalogComponent, canActivate: [ownerGuardFn] },
      { path: "members", component: MembersComponent, canActivate: [ownerGuardFn] },
    ],
  },
  { path: "**", redirectTo: "" },
];
