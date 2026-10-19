import { CurrencyPipe, DatePipe, NgIf } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, tap } from "rxjs";

import { ErrorResponse } from "../../util/model/error-response";
import { AssistantService } from "../assistant-service";
import { AssistantStatus, CheckReply } from "../model/assistant";

/**
 * The Owner's choice to use the Design Assistant (ADR 0004): off until opted
 * in, saying what is sent where, within a monthly budget in US dollars.
 */
@Component({
  selector: "assistant-settings",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, NgIf],
  styleUrls: ["./assistant-settings.component.css"],
  templateUrl: "./assistant-settings.component.html",
})
export class AssistantSettingsComponent {
  status?: AssistantStatus;
  enabled = false;
  budget = 20;
  styleText = "";
  readonly maxStyleLength = 2000;
  saving = false;
  checking = false;
  checkReply: CheckReply | null = null;
  message = "";
  error = "";

  constructor(private assistantService: AssistantService) {
    assistantService.status()
      .pipe(tap(status => this.load(status)))
      .subscribe({ error: () => this.error = $localize`:@@assistant.loadError:Impossible de lire l'état de l'assistant.` });
  }

  get dirty(): boolean {
    return !!this.status && (this.enabled !== this.status.enabled || this.budget !== (this.status.monthlyBudgetUsd ?? 20)
      || this.styleText.trim() !== (this.status.styleText ?? ""));
  }

  get budgetSpent(): boolean {
    return !!this.status?.enabled && this.status.monthlyBudgetUsd !== null
      && this.status.spentThisMonthUsd >= this.status.monthlyBudgetUsd;
  }

  save() {
    this.saving = true;
    this.error = "";
    this.message = "";
    this.assistantService.update(this.enabled, this.budget, this.styleText)
      .pipe(tap(status => {
        this.load(status);
        this.message = $localize`:@@assistant.saved:Assistant enregistré.`;
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
        $localize`:@@assistant.saveError:Impossible d'enregistrer l'assistant.`) });
  }

  check() {
    this.checking = true;
    this.error = "";
    this.checkReply = null;
    this.assistantService.check()
      .pipe(tap(reply => this.checkReply = reply))
      .pipe(finalize(() => this.checking = false))
      .subscribe({
        complete: () => this.assistantService.status().pipe(tap(status => this.status = status)).subscribe(),
        error: (response: HttpErrorResponse) => this.error = this.errorOf(response,
          $localize`:@@assistant.checkError:L'assistant n'a pas répondu.`),
      });
  }

  private load(status: AssistantStatus) {
    this.status = status;
    this.enabled = status.enabled;
    this.budget = status.monthlyBudgetUsd ?? 20;
    this.styleText = status.styleText ?? "";
  }

  private errorOf(response: HttpErrorResponse, fallback: string): string {
    return (response.error as ErrorResponse | null)?.message ?? fallback;
  }
}
