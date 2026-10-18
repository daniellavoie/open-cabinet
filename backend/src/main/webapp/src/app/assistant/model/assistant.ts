/**
 * What a Member sees of the Design Assistant: whether the server can reach a
 * model, whether the Shop opted in, and the month's spending, in US dollars.
 */
export class AssistantStatus {
  configured!: boolean;
  enabled!: boolean;
  monthlyBudgetUsd!: number | null;
  spentThisMonthUsd!: number;
  enabledByName!: string | null;
  enabledTimestamp!: string | null;
  builderModel!: string;
  sorterModel!: string;
}

export class CheckReply {
  model!: string;
  text!: string;
  costUsd!: number;
}
