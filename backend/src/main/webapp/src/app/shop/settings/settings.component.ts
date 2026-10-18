import { NgFor, NgIf } from "@angular/common";
import { ChangeDetectionStrategy, Component, ViewChild } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, tap } from "rxjs";

import { AssistantSettingsComponent } from "../../assistant/assistant-settings/assistant-settings.component";
import { CatalogService } from "../../catalog/catalog-service";
import { Material, MaterialKind } from "../../catalog/model/material";
import { MemberService } from "../../member/member-service";
import { StaleDraftsComponent } from "../../quote/stale-drafts/stale-drafts.component";
import { LABOUR_METHOD_LABELS } from "../../util/labels";
import { LengthInputComponent } from "../../util/length-input/length-input.component";
import { Units } from "../../util/units/units";
import { ConstructionStandard, LabourMethod, ShopSettings } from "../model/shop";
import { ShopService } from "../shop-service";

type StandardMaterialField = keyof Pick<ConstructionStandard,
  "carcassMaterialId" | "backMaterialId" | "frontMaterialId" | "carcassEdgeBandingId" | "frontEdgeBandingId" | "orderedFrontId" | "crownMouldingId"
  | "hingeId" | "shelfPinId" | "legId" | "pullId" | "drawerSlideId" | "drawerBoxId">;

type StandardLengthField = keyof Pick<ConstructionStandard, "doorGapMm" | "toeKickHeightMm" | "kerfMm" | "sheetTrimMm" | "minOffcutMm">;

@Component({
  selector: "settings",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [AssistantSettingsComponent, FormsModule, LengthInputComponent, NgFor, NgIf, StaleDraftsComponent],
  styleUrls: ["./settings.component.css"],
  templateUrl: "./settings.component.html",
})
export class SettingsComponent {
  readonly labourMethods = Object.entries(LABOUR_METHOD_LABELS) as [LabourMethod, string][];

  readonly materialFields: { field: StandardMaterialField; kind: MaterialKind; label: string }[] = [
    { field: "carcassMaterialId", kind: "SHEET", label: $localize`:@@settings.carcass:Caisson` },
    { field: "backMaterialId", kind: "SHEET", label: $localize`:@@settings.back:Dos` },
    { field: "frontMaterialId", kind: "SHEET", label: $localize`:@@settings.front:Façades` },
    { field: "carcassEdgeBandingId", kind: "EDGE_BANDING", label: $localize`:@@settings.carcassBanding:Bande de chant du caisson` },
    { field: "frontEdgeBandingId", kind: "EDGE_BANDING", label: $localize`:@@settings.frontBanding:Bande de chant des façades` },
    { field: "orderedFrontId", kind: "FRONT", label: $localize`:@@settings.orderedFront:Façades commandées` },
    { field: "crownMouldingId", kind: "MOULDING", label: $localize`:@@settings.crownMoulding:Moulure couronne` },
    { field: "hingeId", kind: "HARDWARE", label: $localize`:@@settings.hinge:Charnière` },
    { field: "shelfPinId", kind: "HARDWARE", label: $localize`:@@settings.shelfPin:Taquet de tablette` },
    { field: "legId", kind: "HARDWARE", label: $localize`:@@settings.leg:Patte` },
    { field: "pullId", kind: "HARDWARE", label: $localize`:@@settings.pull:Poignée` },
    { field: "drawerSlideId", kind: "HARDWARE", label: $localize`:@@settings.slide:Coulisse` },
    { field: "drawerBoxId", kind: "HARDWARE", label: $localize`:@@settings.drawerBox:Boîte de tiroir` },
  ];

  readonly lengthFields: { field: StandardLengthField; label: string }[] = [
    { field: "doorGapMm", label: $localize`:@@settings.doorGap:Jeu des façades` },
    { field: "toeKickHeightMm", label: $localize`:@@settings.toeKick:Hauteur du coup de pied` },
    { field: "kerfMm", label: $localize`:@@settings.kerf:Trait de scie` },
    { field: "sheetTrimMm", label: $localize`:@@settings.trim:Rognage des panneaux` },
    { field: "minOffcutMm", label: $localize`:@@settings.minOffcut:Chute minimale` },
  ];

  @ViewChild(StaleDraftsComponent) staleDrafts?: StaleDraftsComponent;

  name = "";
  currency = "";
  units: Units = "IMPERIAL";
  settings?: ShopSettings;
  materials: Material[] = [];
  saving = false;
  message = "";
  error = "";

  constructor(
    private memberService: MemberService,
    private shopService: ShopService,
    catalogService: CatalogService,
  ) {
    const shop = this.memberService.shop()!;
    this.name = shop.name;
    this.currency = shop.currency;
    this.units = shop.units;
    this.settings = structuredClone(shop.settings);

    catalogService.findAll()
      .pipe(tap(materials => this.materials = materials))
      .subscribe();
  }

  materialsOf(kind: MaterialKind): Material[] {
    return this.materials.filter(material => material.kind === kind);
  }

  addTax() {
    this.settings!.taxes = [...this.settings!.taxes, { name: "", ratePercent: 0 }];
  }

  removeTax(index: number) {
    this.settings!.taxes = this.settings!.taxes.filter((_, position) => position !== index);
  }

  save() {
    this.saving = true;
    this.message = "";
    this.error = "";
    this.shopService.update({ name: this.name, currency: this.currency, units: this.units, settings: this.settings! })
      .pipe(tap(shop => {
        this.memberService.setShop(shop);
        this.message = $localize`:@@settings.saved:Paramètres enregistrés.`;
        this.staleDrafts?.check();
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: response => this.error = response.error?.message ?? $localize`:@@settings.saveError:Impossible d'enregistrer les paramètres.` });
  }

}
