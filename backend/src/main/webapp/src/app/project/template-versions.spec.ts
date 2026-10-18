import templates from "../../../../resources/cabinet-templates.json";

import { addCabinet } from "./design-edits";
import { Cabinet, CabinetTemplate, Design } from "./model/design";
import { changeVersions, latestVersions, outdatedCabinets, templateOf, updateToLatest } from "./template-versions";

/** The starter library as first published, so the made-up version 2 below is the only newer one. */
const STARTER = (templates as CabinetTemplate[]).filter(template => template.version === 1);
const BASE_DOOR_1 = STARTER.find(template => template.code === "BASE_DOOR")!;
/** A made-up second version: narrower, so wide Cabinets no longer fit. */
const BASE_DOOR_2: CabinetTemplate = { ...BASE_DOOR_1, version: 2, changeNote: "Narrower doors", maxWidthMm: 609.6 };
const TEMPLATES = [...STARTER, BASE_DOOR_2];

function cabinet(id: string, offsetMm: number, widthMm: number): Cabinet {
  return {
    id, templateCode: "BASE_DOOR", templateVersion: 1, wallId: "w1", offsetMm, widthMm,
    heightMm: 876.3, depthMm: 609.6, elevationMm: 0, shelfCount: 2, finishedLeft: true,
  };
}

function room(): Design {
  return {
    walls: [{ id: "w1", lengthMm: 3657.6, heightMm: 2438.4, angleToNextDegrees: 90 }],
    cabinets: [cabinet("narrow", 0, 457.2), cabinet("wide", 457.2, 762)],
  };
}

describe("template versions", () => {
  it("adds new Cabinets from each template's newest version, in library order", () => {
    const latest = latestVersions(TEMPLATES);
    expect(latest.map(template => template.code)).toEqual(STARTER.map(template => template.code));
    expect(latest[0]).toBe(BASE_DOOR_2);

    const design = addCabinet({ ...room(), cabinets: [] }, latest[0], "w1", TEMPLATES).design;
    expect(design.cabinets[0].templateVersion).toBe(2);
  });

  it("finds the Cabinets on an older version", () => {
    expect(outdatedCabinets(room(), TEMPLATES).map(item => item.id)).toEqual(["narrow", "wide"]);
    expect(outdatedCabinets(room(), STARTER)).toEqual([]);
  });

  it("updates what still fits, keeps the Cabinet's own values and refuses the rest", () => {
    const result = updateToLatest(room(), TEMPLATES, ["narrow", "wide"]);

    const [narrow, wide] = result.design.cabinets;
    expect(narrow).toEqual({ ...cabinet("narrow", 0, 457.2), templateVersion: 2 });
    expect(templateOf(TEMPLATES, narrow)).toBe(BASE_DOOR_2);
    expect(wide.templateVersion).toBe(1);
    expect(result.refused).toEqual([
      { cabinetId: "wide", version: 2, violations: [{ code: "WIDTH_OUT_OF_RANGE", wallId: "w1", cabinetId: "wide" }] },
    ]);
  });

  it("reverts to an earlier version", () => {
    const updated = updateToLatest(room(), TEMPLATES, ["narrow"]).design;
    const reverted = changeVersions(updated, TEMPLATES, [{ cabinetId: "narrow", version: 1 }]);

    expect(reverted.refused).toEqual([]);
    expect(reverted.design).toEqual(room());
  });
});
