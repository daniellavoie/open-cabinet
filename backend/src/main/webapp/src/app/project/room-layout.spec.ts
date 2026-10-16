import { Cabinet, Wall } from "./model/design";
import { layoutWalls, snapOffset } from "./room-layout";

const wall = (id: string, lengthMm: number): Wall => ({ id, lengthMm, heightMm: 2400, angleToNextDegrees: 90 });
const cabinet = (id: string, offsetMm: number, widthMm: number): Cabinet => ({
  id, templateCode: "BASE_DOOR", wallId: "w1", offsetMm, widthMm, heightMm: 876.3, depthMm: 609.6, elevationMm: 0, shelfCount: 1,
});

describe("layoutWalls", () => {
  it("chains walls end to end, turning by each angle", () => {
    const [first, second] = layoutWalls([wall("w1", 3000), wall("w2", 2000)]);

    expect(first.start).toEqual({ x: 0, z: 0 });
    expect(second.start.x).toBeCloseTo(3000);
    expect(second.start.z).toBeCloseTo(0);
    expect(second.direction.x).toBeCloseTo(0);
    expect(second.direction.z).toBeCloseTo(1);
    expect(first.normal.z).toBeCloseTo(1);
  });
});

describe("snapOffset", () => {
  it("snaps to a neighbour's side when close", () => {
    const neighbour = cabinet("a", 0, 457.2);
    expect(snapOffset(cabinet("b", 470, 457.2), 470, wall("w1", 3000), [neighbour], 12.7, 30)).toBe(457.2);
  });

  it("snaps to the grid otherwise", () => {
    expect(snapOffset(cabinet("b", 1000, 457.2), 1003, wall("w1", 3000), [], 12.7, 30)).toBe(1003.3);
  });
});
