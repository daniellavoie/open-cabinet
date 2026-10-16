import { Units } from "../../util/units/units";

export type LabourMethod = "OPERATION" | "LINEAR_FOOT" | "HOURLY";

export class Tax {
  name!: string;
  ratePercent!: number;
}

export class LabourSettings {
  defaultMethod!: LabourMethod;
  ratePerSheet!: number;
  ratePerEdgeBandingMeter!: number;
  ratePerCabinet!: number;
  ratePerHardwareItem!: number;
  ratePerLinearFoot!: number;
  hourlyRate!: number;
}

export class ConstructionStandard {
  carcassMaterialId!: string;
  backMaterialId!: string;
  frontMaterialId!: string;
  carcassEdgeBandingId!: string;
  frontEdgeBandingId!: string;
  hingeId!: string;
  shelfPinId!: string;
  legId!: string;
  pullId!: string;
  drawerSlideId!: string;
  drawerBoxId!: string;
  doorGapMm!: number;
  toeKickHeightMm!: number;
  kerfMm!: number;
  sheetTrimMm!: number;
}

export class ShopSettings {
  markupPercent!: number;
  taxes!: Tax[];
  labour!: LabourSettings;
  constructionStandard!: ConstructionStandard;
}

export class Shop {
  id!: string;
  name!: string;
  currency!: string;
  units!: Units;
  settings!: ShopSettings;
}
