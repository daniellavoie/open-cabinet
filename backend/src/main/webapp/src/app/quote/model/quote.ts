import { LabourMethod } from "../../shop/model/shop";

export type QuoteStatus = "DRAFT" | "SENT" | "ACCEPTED" | "DECLINED";

export class QuoteLine {
  kind!: "CABINETS" | "MANUAL";
  description!: string;
  amount!: number;
}

export class TaxLine {
  name!: string;
  ratePercent!: number;
  amount!: number;
}

export class QuoteBreakdown {
  materials!: number;
  labourMethod!: LabourMethod;
  labour!: number;
  markupPercent!: number;
  markup!: number;
  sheets!: number;
  edgeBandingMeters!: number;
  hardwareItems!: number;
  linearFeet!: number;
  labourHours!: number;
}

export class QuoteContent {
  shopName!: string;
  clientName!: string;
  projectName!: string;
  currency!: string;
  cabinetCount!: number;
  lines!: QuoteLine[];
  subtotal!: number;
  taxes!: TaxLine[];
  total!: number;
  breakdown!: QuoteBreakdown;
}

export class Quote {
  id!: string;
  projectId!: string;
  number!: number;
  displayNumber!: string;
  status!: QuoteStatus;
  content!: QuoteContent;
  creationTimestamp!: string;
  sentTimestamp!: string | null;
}
