import { filesProblem, MAX_FILES, sizeLabel } from "./attachment-files";

function file(name: string, type: string, size = 10): File {
  return new File([new Uint8Array(size)], name, { type });
}

describe("attachment files", () => {
  it("takes photos and PDFs within the limits", () => {
    expect(filesProblem([file("cuisine.jpg", "image/jpeg"), file("plan.pdf", "application/pdf")])).toBeNull();
  });

  it("refuses other types, too many files and too much in all", () => {
    expect(filesProblem([file("notes.txt", "text/plain")])).toContain("notes.txt");
    expect(filesProblem(Array.from({ length: MAX_FILES + 1 }, (_, index) => file(`${index}.png`, "image/png")))).toContain("10");
    expect(filesProblem([file("a.pdf", "application/pdf", 13 * 1024 * 1024), file("b.pdf", "application/pdf", 13 * 1024 * 1024)]))
      .toContain("25");
  });

  it("writes sizes for people", () => {
    expect(sizeLabel(340 * 1024, "fr")).toBe("340 Ko");
    expect(sizeLabel(2.4 * 1024 * 1024, "fr")).toBe("2,4 Mo");
  });
});
