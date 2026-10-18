import { LabourMethod } from "../../shop/model/shop";
import { Design } from "./design";

export class ManualLine {
  description!: string;
  amount!: number;
}

/**
 * How sheets are first cut: ripped along their length into strips, or crosscut into sections.
 */
export type NestingLayout = "RIP_FIRST" | "CROSSCUT_FIRST";

export class Project {
  id!: string;
  clientId!: string;
  name!: string;
  design!: Design;
  labourMethod!: LabourMethod | null;
  labourHours!: number | null;
  /** Null keeps the layout with the fewest sheets for each material. */
  nestingLayout?: NestingLayout | null;
  manualLines!: ManualLine[];
  creationTimestamp!: string;
  updateTimestamp!: string;
}
