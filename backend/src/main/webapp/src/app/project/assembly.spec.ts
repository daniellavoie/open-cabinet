import assemblyCases from "../../../../../test/resources/template-assembly/cases.json";
import ruleCases from "../../../../../test/resources/template-assembly/rules.json";

import { assemble } from "./assembly";
import { CabinetTemplate, MaterialRef, MaterialRole, StandardValue, TemplateAssembly } from "./model/design";
import { checkTemplate, SheetSize, TemplateViolation } from "./template-rules";

interface AssemblyCase {
  name: string;
  assembly: TemplateAssembly;
  context: {
    width: number; height: number; depth: number; shelfCount: number; dividers: number[];
    standard: Partial<Record<StandardValue, number>>; thicknesses: Partial<Record<MaterialRole, number>>;
  };
  parts: { id: string; role: string; box: number[] }[];
  hardware: { role: string; quantity: number }[];
  violations: { code: string; partId: string }[];
}

interface RuleCase {
  name: string;
  template: CabinetTemplate;
  shop: {
    standard: Partial<Record<StandardValue, number>>; thicknesses: Partial<Record<MaterialRole, number>>;
    sheets: Partial<Record<MaterialRole, SheetSize>>; trimMm: number;
  };
  violations: TemplateViolation[];
}

const round = (millimetres: number) => Math.round(millimetres * 10) / 10;

/**
 * Runs the shared assembly and template rules cases, the same files the
 * server's AssemblyTest and TemplateRulesTest run (ADR 0006).
 */
describe("assemble (shared cases)", () => {
  for (const testCase of assemblyCases as unknown as AssemblyCase[]) {
    it(testCase.name, () => {
      const { thicknesses, ...context } = testCase.context;
      const result = assemble(testCase.assembly, {
        ...context,
        thickness: (material: MaterialRef) => thicknesses[material.role!] ?? 0,
      });

      expect(result.violations).toEqual(testCase.violations);
      expect(result.parts.map(part => ({
        id: part.id, role: part.role,
        box: [part.box.x0, part.box.x1, part.box.y0, part.box.y1, part.box.z0, part.box.z1].map(round),
      }))).toEqual(testCase.parts);
      expect(result.hardware.map(item => ({ role: item.material.role, quantity: item.quantity }))).toEqual(testCase.hardware);
    });
  }
});

describe("checkTemplate (shared cases)", () => {
  for (const testCase of ruleCases as unknown as RuleCase[]) {
    it(testCase.name, () => {
      const shop = testCase.shop;
      expect(checkTemplate(testCase.template, {
        standard: shop.standard,
        thickness: material => shop.thicknesses[material.role!] ?? 0,
        sheet: material => shop.sheets[material.role!] ?? null,
        trimMm: shop.trimMm,
      })).toEqual(testCase.violations);
    });
  }
});
