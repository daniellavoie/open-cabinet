import { LabourMethod } from "../../shop/model/shop";
import { Design } from "./design";

export class ManualLine {
  description!: string;
  amount!: number;
}

export class Project {
  id!: string;
  clientId!: string;
  name!: string;
  design!: Design;
  labourMethod!: LabourMethod | null;
  labourHours!: number | null;
  manualLines!: ManualLine[];
  creationTimestamp!: string;
  updateTimestamp!: string;
}
