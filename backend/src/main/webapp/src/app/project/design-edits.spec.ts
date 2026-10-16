import templates from "../../../../resources/cabinet-templates.json";

import { checkConstructionRules } from "./construction-rules";
import { addCabinet, addWall, removeWall } from "./design-edits";
import { CabinetTemplate, Design } from "./model/design";

const TEMPLATES = templates as CabinetTemplate[];
const BASE_DOOR = TEMPLATES.find(template => template.code === "BASE_DOOR")!;
const WALL_DOOR = TEMPLATES.find(template => template.code === "WALL_DOOR")!;

function room(): Design {
  return { walls: [{ id: "w1", lengthMm: 3657.6, heightMm: 2438.4, angleToNextDegrees: 90 }], cabinets: [] };
}

describe("addCabinet", () => {
  it("places new cabinets side by side without breaking the rules", () => {
    let design = room();
    for (let index = 0; index < 3; index++) {
      design = addCabinet(design, BASE_DOOR, "w1", TEMPLATES).design;
    }
    design = addCabinet(design, WALL_DOOR, "w1", TEMPLATES).design;

    expect(design.cabinets.map(cabinet => cabinet.offsetMm)).toEqual([0, 457.2, 914.4, 0]);
    expect(checkConstructionRules(design, TEMPLATES)).toEqual([]);
  });
});

describe("walls", () => {
  it("adds walls and removes them with their cabinets, keeping at least one", () => {
    let design = addWall(room());
    const secondWall = design.walls[1].id;
    design = addCabinet(design, BASE_DOOR, secondWall, TEMPLATES).design;

    design = removeWall(design, secondWall);
    expect(design.walls).toHaveLength(1);
    expect(design.cabinets).toHaveLength(0);
    expect(removeWall(design, "w1").walls).toHaveLength(1);
  });
});
