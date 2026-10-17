import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { RouterLink } from "@angular/router";
import { tap } from "rxjs";

import { Quote } from "../model/quote";
import { QuoteService } from "../quote-service";

/**
 * After an Owner changes prices or settings: the draft Quotes that still
 * carry the old ones. Parents call check() once the change is saved.
 */
@Component({
  selector: "stale-drafts",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [NgFor, NgIf, RouterLink],
  styleUrls: ["./stale-drafts.component.css"],
  templateUrl: "./stale-drafts.component.html",
})
export class StaleDraftsComponent {
  drafts: Quote[] = [];

  constructor(private quoteService: QuoteService) {}

  check() {
    this.quoteService.findDrafts()
      .pipe(tap(drafts => this.drafts = drafts))
      .subscribe();
  }
}
