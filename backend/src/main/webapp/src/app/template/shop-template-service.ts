import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { Cabinet } from "../project/model/design";

/**
 * A Cabinet Template a Shop saved from a designed Cabinet: a named preset of
 * a starter template (`baseCode`). Cabinets added from it keep the base code.
 */
export class ShopCabinetTemplate {
  id!: string;
  name!: string;
  baseCode!: string;
  preset!: Omit<Cabinet, "id" | "wallId">;
}

@Injectable({ providedIn: "root" })
export class ShopTemplateService {
  constructor(private httpClient: HttpClient) {}

  findAll(): Observable<ShopCabinetTemplate[]> {
    return this.httpClient.get<ShopCabinetTemplate[]>("/api/shop-template");
  }

  create(name: string, cabinet: Cabinet): Observable<ShopCabinetTemplate> {
    return this.httpClient.post<ShopCabinetTemplate>("/api/shop-template", { name, cabinet });
  }

  delete(templateId: string): Observable<void> {
    return this.httpClient.delete<void>(`/api/shop-template/${templateId}`);
  }
}
