import cases from "../../../../../test/resources/design-edits/cases.json";
import templates from "../../../../resources/cabinet-templates.json";

import {
  addAppliance, addCabinet, addDivider, addFiller, addOpening, addWall, moveDivider, removeCabinet, removeDivider,
  removeItem, removeWall, updateCabinet, updateItem, updateWall,
} from "./design-edits";
import { ApplianceKind, CabinetTemplate, Design, OpeningKind } from "./model/design";

type Args = Record<string, any>;

const all = templates as CabinetTemplate[];

/** Each edit, with the id the case gives to what it creates. */
const ACTIONS: Record<string, (design: Design, args: Args) => Design> = {
  addWall: (design, args) => renameLast(addWall(design), "walls", args["id"]),
  updateWall: (design, args) => updateWall(design, args["wallId"], args["changes"]),
  removeWall: (design, args) => removeWall(design, args["wallId"]),
  addFiller: (design, args) => renameLast(addFiller(design, args["wallId"]).design, "fillers", args["id"]),
  addOpening: (design, args) =>
    renameLast(addOpening(design, args["kind"] as OpeningKind, args["wallId"]).design, "openings", args["id"]),
  addAppliance: (design, args) =>
    renameLast(addAppliance(design, args["kind"] as ApplianceKind, args["wallId"]).design, "appliances", args["id"]),
  updateItem: (design, args) => updateItem(design, args["itemId"], args["changes"]),
  removeItem: (design, args) => removeItem(design, args["itemId"]),
  addCabinet: (design, args) => {
    const template = all.find(item => item.code === args["templateCode"] && item.version === args["templateVersion"])!;
    return renameLast(addCabinet(design, template, args["wallId"], all).design, "cabinets", args["id"]);
  },
  updateCabinet: (design, args) => updateCabinet(design, args["cabinetId"], args["changes"]),
  addDivider: (design, args) => addDivider(design, args["cabinetId"]),
  moveDivider: (design, args) => moveDivider(design, args["cabinetId"], args["index"], args["positionMm"]),
  removeDivider: (design, args) => removeDivider(design, args["cabinetId"], args["index"]),
  removeCabinet: (design, args) => removeCabinet(design, args["cabinetId"]),
};

function renameLast(design: Design, list: "walls" | "cabinets" | "openings" | "appliances" | "fillers", id: string): Design {
  const items = [...(design[list] ?? [])] as { id: string }[];
  items[items.length - 1] = { ...items[items.length - 1], id };
  return { ...design, [list]: items };
}

/**
 * Runs the shared design edit cases, the same file the server's
 * DesignEditsTest runs: the Design Assistant replays these edits on the
 * server (docs/adr/0004-design-assistant-proposes-member-accepts.md).
 */
describe("design edits (shared cases)", () => {
  it("covers every edit", () => {
    expect(Object.keys(ACTIONS).sort()).toEqual([...new Set((cases as { action: string }[]).map(item => item.action))].sort());
  });

  for (const testCase of cases as { name: string; action: string; args: Args; design: Design; expected: Design }[]) {
    it(testCase.name, () => {
      expect(ACTIONS[testCase.action](testCase.design, testCase.args)).toEqual(testCase.expected);
    });
  }
});
