import { HttpClient } from "@angular/common/http";
import { Inject, Injectable, LOCALE_ID } from "@angular/core";
import { Observable } from "rxjs";

import { outputLanguage } from "../util/download";

/**
 * A Shop Drawing ready to show: what it shows, its title and a standalone SVG document.
 */
export class DrawingView {
  kind!: "PLAN" | "ELEVATION" | "CABINET";
  title!: string;
  svg!: string;
}

@Injectable({ providedIn: "root" })
export class DrawingService {
  private readonly language: string;

  constructor(private httpClient: HttpClient, @Inject(LOCALE_ID) locale: string) {
    this.language = outputLanguage(locale);
  }

  shopDrawings(projectId: string): Observable<DrawingView[]> {
    return this.httpClient.get<DrawingView[]>(`/api/project/${projectId}/drawing`, { params: { lang: this.language } });
  }

  shopDrawingsPdf(projectId: string): Observable<Blob> {
    return this.httpClient.get(`/api/project/${projectId}/drawing/pdf`, { params: { lang: this.language }, responseType: "blob" });
  }
}
