import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { map, Observable } from "rxjs";

import { Page } from "../util/model/page";
import { Client } from "./model/client";

export type ClientRequest = Omit<Client, "id">;

@Injectable({ providedIn: "root" })
export class ClientService {
  constructor(private httpClient: HttpClient) {}

  findAll(): Observable<Client[]> {
    return this.httpClient.get<Page<Client>>("/api/client?size=1000&sort=name,ASC")
      .pipe(map(page => page.content));
  }

  create(request: ClientRequest): Observable<Client> {
    return this.httpClient.post<Client>("/api/client", request);
  }

  update(clientId: string, request: ClientRequest): Observable<Client> {
    return this.httpClient.put<Client>(`/api/client/${clientId}`, request);
  }
}
