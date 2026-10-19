import { Remark } from "../assistant/model/conversation";
import { remarkLabel } from "./labels";

function remark(code: string, measureMm: number | null = null, limitMm: number | null = null): Remark {
  return { code, wallId: "w2", ids: ["cabinet-1", "cabinet-2"], measureMm, limitMm };
}

describe("remark labels", () => {
  it("say the wall and the measure in the Shop's units, without the assistant's ids", () => {
    const imperial = remarkLabel(remark("GAP", 41.3, 76.2), 2, "IMPERIAL");
    const metric = remarkLabel(remark("GAP", 41.3, 75), 2, "METRIC");

    expect(imperial).toContain("Mur 2");
    expect(imperial).toContain("1 5/8");
    expect(imperial).not.toContain("cabinet-1");
    expect(metric).toContain("41");
  });

  it("read a top line above or below the others the same way", () => {
    expect(remarkLabel(remark("UPPER_TOP_LINE", -50.8), 1, "IMPERIAL")).toContain("2");
    expect(remarkLabel(remark("UPPER_TOP_LINE", -50.8), 1, "IMPERIAL")).not.toContain("-");
  });

  it("name the triangle without a wall", () => {
    expect(remarkLabel({ ...remark("WORK_TRIANGLE", 3000, 2743.2), wallId: null }, null, "IMPERIAL")).not.toContain("Mur");
  });
});
