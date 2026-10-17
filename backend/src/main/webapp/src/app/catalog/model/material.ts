export type MaterialKind = "SHEET" | "EDGE_BANDING" | "HARDWARE" | "FRONT" | "MOULDING";

export class Material {
  id!: string;
  kind!: MaterialKind;
  name!: string;
  sku!: string | null;
  price!: number;
  thicknessMm!: number | null;
  lengthMm!: number | null;
  widthMm!: number | null;
  grain!: boolean;
}
