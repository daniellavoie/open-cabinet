import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { Shop, ShopSettings } from "./model/shop";
import { Units } from "../util/units/units";

export class CreateShopRequest {
  shopName!: string;
  memberName!: string;
  memberEmail!: string | null;
}

export class UpdateShopRequest {
  name!: string;
  currency!: string;
  units!: Units;
  settings!: ShopSettings;
}

@Injectable({ providedIn: "root" })
export class ShopService {
  constructor(private httpClient: HttpClient) {}

  create(request: CreateShopRequest): Observable<Shop> {
    return this.httpClient.post<Shop>("/api/shop", request);
  }

  update(request: UpdateShopRequest): Observable<Shop> {
    return this.httpClient.put<Shop>("/api/shop", request);
  }
}
