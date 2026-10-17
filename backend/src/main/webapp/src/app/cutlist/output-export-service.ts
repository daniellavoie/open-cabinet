import { HttpClient } from "@angular/common/http";
import { Inject, Injectable, LOCALE_ID } from "@angular/core";
import { Observable } from "rxjs";

import { outputLanguage } from "../util/download";

export type OutputKind = "cut-list" | "nesting" | "bill-of-materials";

export type ExportFormat = "pdf" | "csv";

/**
 * The Cut List, Nesting and Bill of Materials of a saved Project, as files to print or open in a
 * spreadsheet, written in the app's language.
 */
@Injectable({ providedIn: "root" })
export class OutputExportService {
  private readonly language: string;

  constructor(private httpClient: HttpClient, @Inject(LOCALE_ID) locale: string) {
    this.language = outputLanguage(locale);
  }

  download(projectId: string, kind: OutputKind, format: ExportFormat): Observable<Blob> {
    return this.httpClient.get(`/api/project/${projectId}/output/${kind}/${format}`, {
      params: { lang: this.language },
      responseType: "blob",
    });
  }
}
