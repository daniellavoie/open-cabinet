import { Cabinet, Design } from "./model/design";
import { countMarks, proposalMarks } from "./proposal-marks";

function cabinet(id: string, offsetMm: number, wallId = "w1"): Cabinet {
  return {
    id, templateCode: "BASE_DOOR", templateVersion: 2, wallId, offsetMm, widthMm: 457.2, heightMm: 876.3,
    depthMm: 609.6, elevationMm: 0, shelfCount: 1,
  };
}

const walls = [
  { id: "w1", lengthMm: 3000, heightMm: 2400, angleToNextDegrees: 90 },
  { id: "w2", lengthMm: 2000, heightMm: 2400, angleToNextDegrees: 90 },
];

describe("proposal marks", () => {
  it("marks what the proposal adds, changes and removes", () => {
    const live: Design = { walls, cabinets: [cabinet("kept", 0), cabinet("moved", 500), cabinet("gone", 1000)] };
    const proposal: Design = {
      walls,
      cabinets: [cabinet("kept", 0), cabinet("moved", 600), cabinet("new", 2000)],
      fillers: [{ id: "f1", wallId: "w1", offsetMm: 457.2, widthMm: 42.8, heightMm: 774.7, depthMm: 609.6, elevationMm: 101.6 }],
    };

    const { design, marks } = proposalMarks(live, proposal);

    expect([...marks.entries()]).toEqual([["moved", "CHANGED"], ["new", "ADDED"], ["gone", "REMOVED"], ["f1", "ADDED"]]);
    expect(design.cabinets.map(item => item.id)).toEqual(["kept", "moved", "new", "gone"]);
    expect(countMarks(marks)).toEqual({ ADDED: 2, CHANGED: 1, REMOVED: 1 });
  });

  it("leaves out what was on a wall the proposal removes", () => {
    const live: Design = { walls, cabinets: [cabinet("c1", 0, "w2")] };
    const proposal: Design = { walls: [walls[0]], cabinets: [] };

    const { design, marks } = proposalMarks(live, proposal);

    expect(design.cabinets).toEqual([]);
    expect(marks.size).toBe(0);
  });
});
