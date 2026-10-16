import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

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

  delete(materialId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/material/${materialId}`);
  }
}
