import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { map, Observable, shareReplay } from "rxjs";

import { OutputDocument, ProjectOutput } from "../cutlist/model/project-output";
import { Page } from "../util/model/page";
import { CabinetTemplate } from "./model/design";
import { Project } from "./model/project";
import { ProjectLock } from "./model/project-lock";

export type UpdateProjectRequest = Pick<Project, "clientId" | "name" | "design" | "labourMethod" | "labourHours" | "manualLines" | "nestingLayout">;

@Injectable({ providedIn: "root" })
export class ProjectService {
  private templates$?: Observable<CabinetTemplate[]>;

  constructor(private httpClient: HttpClient) {}

  findAll(): Observable<Project[]> {
    return this.httpClient.get<Page<Project>>("/api/project?size=1000")
      .pipe(map(page => page.content));
  }

  findById(projectId: string): Observable<Project> {
    return this.httpClient.get<Project>(`/api/project/${projectId}`);
  }

  create(clientId: string, name: string): Observable<Project> {
    return this.httpClient.post<Project>("/api/project", { clientId, name });
  }

  update(projectId: string, request: UpdateProjectRequest): Observable<Project> {
    return this.httpClient.put<Project>(`/api/project/${projectId}`, request);
  }

  /**
   * Takes or renews the editing lock; reports the holder when someone else has it.
   */
  lock(projectId: string): Observable<ProjectLock> {
    return this.httpClient.post<ProjectLock>(`/api/project/${projectId}/lock`, {});
  }

  unlock(projectId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/project/${projectId}/lock`);
  }

  /**
   * The saved Project's Shop Drawings as a PDF, in `language` (fr or en).
   */
  shopDrawings(projectId: string, language: string): Observable<Blob> {
    return this.httpClient.get(`/api/project/${projectId}/shop-drawings/pdf?language=${language}`, { responseType: "blob" });
  }

  /**
   * The saved Project's Cut List, Nesting or Bill of Materials as a PDF or CSV, in `language` (fr or en).
   */
  outputDocument(projectId: string, document: OutputDocument, format: "pdf" | "csv", language: string): Observable<Blob> {
    return this.httpClient.get(`/api/project/${projectId}/output/${document}/${format}?language=${language}`, { responseType: "blob" });
  }

  output(projectId: string): Observable<ProjectOutput> {
    return this.httpClient.get<ProjectOutput>(`/api/project/${projectId}/output`);
  }

  /**
   * The starter Cabinet Templates, served by the API from the same file the
   * server's Construction Rules use.
   */
  templates(): Observable<CabinetTemplate[]> {
    this.templates$ ??= this.httpClient.get<CabinetTemplate[]>("/api/cabinet-template")
      .pipe(shareReplay(1));
    return this.templates$;
  }
}
