import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { CabinetTemplate } from "../project/model/design";

/**
 * A Cabinet Template designed for one Project (ADR 0006): its editable draft
 * and its published versions, oldest first, which never change. Cabinets use
 * it by its id as their template code.
 */
export class ProjectTemplate {
  id!: string;
  projectId!: string;
  name!: string;
  draft!: CabinetTemplate;
  versions!: CabinetTemplate[];
  /** The Cabinet it was made from, offered the template once published. */
  sourceCabinetId!: string | null;
}

/** Start from a Template Version, or from one of the Project's Cabinets. */
export interface CreateProjectTemplateRequest {
  name: string;
  templateCode?: string;
  templateVersion?: number;
  cabinetId?: string;
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

  /** Publishes the draft as the next version, if it passes the template rules. */
  publish(templateId: string, changeNote: string | null): Observable<ProjectTemplate> {
    return this.httpClient.post<ProjectTemplate>(`/api/project-template/${templateId}/publish`, { changeNote });
  }
}
