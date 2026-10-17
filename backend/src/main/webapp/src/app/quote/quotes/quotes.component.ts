import { CurrencyPipe, DatePipe, DecimalPipe, NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, Observable, tap } from "rxjs";

import { ManualLine, Project } from "../../project/model/project";
import { LabourMethod } from "../../shop/model/shop";
import { LABOUR_METHOD_LABELS, QUOTE_STATUS_LABELS } from "../../util/labels";
import { Quote, QuoteStatus } from "../model/quote";
import { QuoteService } from "../quote-service";

/**
 * How a Project is priced (Labour Method, hours, Manual Lines) and its Quotes.
 * Quotes are priced by the server from the saved Project.
 */
@Component({
  selector: "quotes",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, FormsModule, NgFor, NgIf],
  styleUrls: ["./quotes.component.css"],
  templateUrl: "./quotes.component.html",
})
export class QuotesComponent implements OnChanges {
  readonly labourMethods = Object.entries(LABOUR_METHOD_LABELS) as [LabourMethod, string][];
  readonly methodLabels = LABOUR_METHOD_LABELS;
  readonly statusLabels = QUOTE_STATUS_LABELS;

  @Input() project!: Project;
  @Input() dirty = false;
  /**
   * Another Member is editing: pricing inputs are locked, quotes can still be managed.
   */
  @Input() readOnly = false;
  @Input() currency = "CAD";
  @Input() defaultLabourMethod: LabourMethod = "OPERATION";
  @Output() projectChange = new EventEmitter<Project>();

  quotes: Quote[] = [];
  selected: Quote | null = null;
  busy = false;
  error = "";

  constructor(private quoteService: QuoteService) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes["project"]?.previousValue?.id !== this.project.id) {
      this.load();
    }
  }

  load() {
    this.quoteService.findByProject(this.project.id)
      .pipe(tap(quotes => {
        this.quotes = quotes;
        this.selected = quotes.find(quote => quote.id === this.selected?.id) ?? quotes[0] ?? null;
      }))
      .subscribe();
  }

  setLabourMethod(method: LabourMethod | "") {
    this.projectChange.emit({ ...this.project, labourMethod: method === "" ? null : method });
  }

  setLabourHours(hours: number | null) {
    this.projectChange.emit({ ...this.project, labourHours: hours });
  }

  addManualLine() {
    this.projectChange.emit({ ...this.project, manualLines: [...this.project.manualLines, { description: "", amount: 0 }] });
  }

  updateManualLine(index: number, changes: Partial<ManualLine>) {
    this.projectChange.emit({
      ...this.project,
      manualLines: this.project.manualLines.map((line, position) => position === index ? { ...line, ...changes } : line),
    });
  }

  removeManualLine(index: number) {
    this.projectChange.emit({ ...this.project, manualLines: this.project.manualLines.filter((_, position) => position !== index) });
  }

  usesHours(): boolean {
    return (this.project.labourMethod ?? this.defaultLabourMethod) === "HOURLY";
  }

  create() {
    this.run(this.quoteService.create(this.project.id), quote => this.selected = quote);
  }

  /**
   * Only the latest revision of a sent Quote can be revised.
   */
  canRevise(quote: Quote): boolean {
    return quote.status !== "DRAFT"
      && !this.quotes.some(other => other.number === quote.number && other.revision > quote.revision);
  }

  revise(quote: Quote) {
    this.run(this.quoteService.revise(quote.id), revision => this.selected = revision);
  }

  send(quote: Quote) {
    this.run(this.quoteService.send(quote.id), sent => this.selected = sent);
  }

  setStatus(quote: Quote, status: QuoteStatus) {
    this.run(this.quoteService.updateStatus(quote.id, status), updated => this.selected = updated);
  }

  remove(quote: Quote) {
    this.run(this.quoteService.delete(quote.id), () => this.selected = null);
  }

  download(quote: Quote) {
    this.quoteService.pdf(quote.id)
      .pipe(tap(blob => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${quote.displayNumber}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      }))
      .subscribe({ error: () => this.error = $localize`:@@quotes.pdfError:Impossible de télécharger la soumission.` });
  }

  private run<T>(request: Observable<T>, then: (result: T) => void) {
    this.busy = true;
    this.error = "";
    request
      .pipe(tap(result => {
        then(result);
        this.load();
      }))
      .pipe(finalize(() => this.busy = false))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@quotes.error:L'opération a échoué.` });
  }
}
