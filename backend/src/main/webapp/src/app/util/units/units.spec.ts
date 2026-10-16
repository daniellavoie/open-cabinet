import { formatLength, parseLength } from "./units";

describe("units", () => {
  it("formats imperial lengths to the nearest sixteenth", () => {
    expect(formatLength(457.2, "IMPERIAL")).toBe('18"');
    expect(formatLength(876.3, "IMPERIAL")).toBe('34 1/2"');
    expect(formatLength(15.875, "IMPERIAL")).toBe('5/8"');
    expect(formatLength(3, "IMPERIAL")).toBe('1/8"');
  });

  it("formats metric lengths in whole millimetres", () => {
    expect(formatLength(457.2, "METRIC")).toBe("457 mm");
  });

  it("parses the ways shops type inches", () => {
    expect(parseLength("18", "IMPERIAL")).toBe(457.2);
    expect(parseLength('34 1/2"', "IMPERIAL")).toBe(876.3);
    expect(parseLength("34-1/2", "IMPERIAL")).toBe(876.3);
    expect(parseLength("34.5", "IMPERIAL")).toBe(876.3);
    expect(parseLength("5/8", "IMPERIAL")).toBe(15.9);
    expect(parseLength("abc", "IMPERIAL")).toBeNull();
    expect(parseLength("1/0", "IMPERIAL")).toBeNull();
  });

  it("parses millimetres", () => {
    expect(parseLength("610", "METRIC")).toBe(610);
    expect(parseLength("610 mm", "METRIC")).toBe(610);
    expect(parseLength("", "METRIC")).toBeNull();
  });
});
