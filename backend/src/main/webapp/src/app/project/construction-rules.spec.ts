import cases from "../../../../../test/resources/construction-rules/cases.json";
import templates from "../../../../resources/cabinet-templates.json";

import { checkConstructionRules } from "./construction-rules";
import { CabinetTemplate, Design, Violation } from "./model/design";

/**
 * Runs the shared Construction Rules cases, the same file the server's
 * ConstructionRulesTest runs (docs/adr/0002-construction-rules-run-in-browser-and-server.md).
 */
describe("checkConstructionRules (shared cases)", () => {
  for (const testCase of cases as { name: string; design: Design; violations: Violation[] }[]) {
    it(testCase.name, () => {
      expect(checkConstructionRules(testCase.design, templates as CabinetTemplate[]))
        .toEqual(testCase.violations);
    });
  }
});
