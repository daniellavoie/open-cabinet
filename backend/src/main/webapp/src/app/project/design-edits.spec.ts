import templates from "../../../../resources/cabinet-templates.json";

import { checkConstructionRules } from "./construction-rules";
import { addAppliance, addCabinet, addDivider, addOpening, addWall, moveDivider, removeDivider, removeItem, removeWall, updateItem } from "./design-edits";
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

describe("corners", () => {
  it("places a cabinet past the neighbouring wall's corner cabinet", () => {
    let design = addWall(room());
    const secondWall = design.walls[1].id;
    design = addCabinet(design, BASE_DOOR, "w1", TEMPLATES).design;
    design = { ...design, cabinets: design.cabinets.map(cabinet => ({ ...cabinet, offsetMm: 3657.6 - 457.2 })) };
    design = addCabinet(design, BASE_DOOR, secondWall, TEMPLATES).design;

    expect(design.cabinets[1].offsetMm).toBeCloseTo(609.6 + 25.4);
    expect(checkConstructionRules(design, TEMPLATES)).toEqual([]);
  });
});

describe("openings and appliances", () => {
  it("fits new cabinets around them", () => {
    let design = addAppliance(room(), "DISHWASHER", "w1").design;
    design = addCabinet(design, BASE_DOOR, "w1", TEMPLATES).design;

    expect(design.cabinets[0].offsetMm).toBeCloseTo(609.6);
    expect(checkConstructionRules(design, TEMPLATES)).toEqual([]);
  });

  it("are edited and removed by id, and go with their wall", () => {
    let design = addWall(room());
    const secondWall = design.walls[1].id;
    const window = addOpening(design, "WINDOW", secondWall);
    design = updateItem(window.design, window.itemId, { offsetMm: 500 });
    expect(design.openings![0].offsetMm).toBe(500);
    expect(removeItem(design, window.itemId).openings).toEqual([]);

    design = addAppliance(design, "FRIDGE", secondWall).design;
    design = removeWall(design, secondWall);
    expect(design.openings).toEqual([]);
    expect(design.appliances).toEqual([]);
  });
});

describe("dividers", () => {
  it("are added in the widest section, moved and removed", () => {
    let design = addCabinet(room(), { ...BASE_DOOR, defaultWidthMm: 914.4 }, "w1", TEMPLATES).design;
    const cabinetId = design.cabinets[0].id;
    design = addDivider(design, cabinetId);
    design = addDivider(design, cabinetId);
    expect(design.cabinets[0].dividerPositionsMm).toEqual([228.6, 457.2]);

    design = moveDivider(design, cabinetId, 0, 300);
    expect(design.cabinets[0].dividerPositionsMm).toEqual([300, 457.2]);
    expect(checkConstructionRules(design, TEMPLATES)).toEqual([]);
    expect(removeDivider(design, cabinetId, 0).cabinets[0].dividerPositionsMm).toEqual([457.2]);
  });
});

describe("shop templates", () => {
  it("add a cabinet with the preset over its base template", () => {
    const preset = { templateCode: "BASE_DOOR", templateVersion: 1, offsetMm: 0, widthMm: 762, heightMm: 876.3, depthMm: 533.4, elevationMm: 0,
      shelfCount: 2, dividerPositionsMm: [381], finishedLeft: true };
    const design = addCabinet(room(), BASE_DOOR, "w1", TEMPLATES, preset).design;

    expect(design.cabinets[0]).toMatchObject({ templateCode: "BASE_DOOR", wallId: "w1", widthMm: 762, depthMm: 533.4,
      shelfCount: 2, dividerPositionsMm: [381], finishedLeft: true });
    expect(design.cabinets[0].id).toMatch(/^cabinet-/);
  });
});
