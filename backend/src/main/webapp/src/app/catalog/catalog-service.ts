import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { CatalogImportResult } from "./model/catalog-import-result";
import { Material } from "./model/material";

export type MaterialRequest = Omit<Material, "id">;

@Injectable({ providedIn: "root" })
export class CatalogService {
  constructor(private httpClient: HttpClient) {}

  findAll(): Observable<Material[]> {
    return this.httpClient.get<Material[]>("/api/material");
  }

  create(request: MaterialRequest): Observable<Material> {
    return this.httpClient.post<Material>("/api/material", request);
  }

  update(materialId: string, request: MaterialRequest): Observable<Material> {
    return this.httpClient.put<Material>(`/api/material/${materialId}`, request);
  }

  exportCsv(): Observable<Blob> {
    return this.httpClient.get("/api/material/export", { responseType: "blob" });
  }

  /**
   * All or nothing: with any error, nothing changes and every error is listed.
   */
  importCsv(csv: string): Observable<CatalogImportResult> {
    return this.httpClient.post<CatalogImportResult>("/api/material/import", csv, { headers: { "Content-Type": "text/csv" } });
  }

  delete(materialId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/material/${materialId}`);
  }
}
