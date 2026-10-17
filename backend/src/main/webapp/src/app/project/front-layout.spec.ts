import { frontRows } from "./front-layout";
import { CabinetTemplate } from "./model/design";

describe("frontRows", () => {
  it("matches FrontLayoutTest: fixed rows, then flexible rows by weight", () => {
    const template = {
      frontRows: [
        { kind: "DRAWER", heightMm: 150 },
        { kind: "DOOR", weight: 1 },
        { kind: "DOOR", weight: 3 },
      ],
    } as CabinetTemplate;

    const rows = frontRows(template, 1009, 3);

    expect(rows.map(row => row.heightMm)).toEqual([150, 212.5, 637.5]);
    expect(rows[0].bottomMm).toBeCloseTo(1009 - 1.5 - 150);
    expect(rows[2].bottomMm).toBeCloseTo(1.5);
  });

  it("drops rows left without height", () => {
    const template = { frontRows: [{ kind: "OPENING", heightMm: 800 }, { kind: "DRAWER" }] } as CabinetTemplate;

    expect(frontRows(template, 700, 3).map(row => row.kind)).toEqual(["OPENING"]);
  });
});
