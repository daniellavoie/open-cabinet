import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { CabinetTemplate, CabinetType } from "../project/model/design";

/**
 * A Cabinet Template a Member designed (ADR 0006): its editable draft and its
 * published versions, oldest first, which never change. Cabinets use it by its
 * id as their template code. It belongs to its Project until shared with the
 * Shop library; templates created in the library have no Project.
 */
export class ProjectTemplate {
  id!: string;
  projectId!: string | null;
  name!: string;
  draft!: CabinetTemplate;
  versions!: CabinetTemplate[];
  /** The Cabinet it was made from, offered the template once published. */
  sourceCabinetId!: string | null;
  shared!: boolean;
  /** Hidden when adding Cabinets; Cabinets using it keep it. */
  archived!: boolean;
}

/**
 * Start from a Template Version, or from one of the Project's Cabinets. In the
 * Shop library, a type alone starts from scratch.
 */
export interface CreateProjectTemplateRequest {
  name: string;
  templateCode?: string;
  templateVersion?: number;
  cabinetId?: string;
  type?: CabinetType;
}

@Injectable({ providedIn: "root" })
export class ProjectTemplateService {
  constructor(private httpClient: HttpClient) {}

  findByProject(projectId: string): Observable<ProjectTemplate[]> {
    return this.httpClient.get<ProjectTemplate[]>(`/api/project/${projectId}/project-template`);
  }

  findById(templateId: string): Observable<ProjectTemplate> {
    return this.httpClient.get<ProjectTemplate>(`/api/project-template/${templateId}`);
  }

  create(projectId: string, request: CreateProjectTemplateRequest): Observable<ProjectTemplate> {
    return this.httpClient.post<ProjectTemplate>(`/api/project/${projectId}/project-template`, request);
  }

  /** Saves the draft as it is; nothing is checked until it is published. */
  update(templateId: string, name: string, draft: CabinetTemplate): Observable<ProjectTemplate> {
    return this.httpClient.put<ProjectTemplate>(`/api/project-template/${templateId}`, { name, draft });
  }

  /** The Shop library: templates every Project of the Shop can use. */
  findShared(): Observable<ProjectTemplate[]> {
    return this.httpClient.get<ProjectTemplate[]>("/api/template");
  }

  /** Starts a template in the Shop library. */
  createInLibrary(request: CreateProjectTemplateRequest): Observable<ProjectTemplate> {
    return this.httpClient.post<ProjectTemplate>("/api/template", request);
  }

  /** Moves a Project template, with its history, into the Shop library; it cannot be un-shared. */
  share(templateId: string): Observable<ProjectTemplate> {
    return this.httpClient.post<ProjectTemplate>(`/api/project-template/${templateId}/share`, {});
  }

  setArchived(templateId: string, archived: boolean): Observable<ProjectTemplate> {
    return this.httpClient.put<ProjectTemplate>(`/api/project-template/${templateId}/archived`, archived);
  }

  /** Removes a template that was never published. */
  delete(templateId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/project-template/${templateId}`);
  }

  /** Publishes the draft as the next version, if it passes the template rules. */
  publish(templateId: string, changeNote: string | null): Observable<ProjectTemplate> {
    return this.httpClient.post<ProjectTemplate>(`/api/project-template/${templateId}/publish`, { changeNote });
  }
}
