import { assemble } from "../project/assembly";
import { TemplateAssembly } from "../project/model/design";
import { addHardware, addPanel, addStack, attach, partNamed, remove, rename, setDivider, uniqueName } from "./template-edits";

const context = {
  width: 600, height: 700, depth: 500, shelfCount: 1, dividers: [],
  standard: { DOOR_GAP: 3, TOE_KICK_HEIGHT: 100 }, thickness: () => 20,
};

describe("template edits", () => {
  it("adds panels that can be built right away, each with its own name", () => {
    let assembly: TemplateAssembly = { parts: [] };
    assembly = addPanel(assembly, "CARCASS", "Tablette").assembly;
    const second = addPanel(assembly, "CARCASS", "Tablette");

    expect(second.id).toBe("panel2");
    const result = assemble(second.assembly, context);
    expect(result.violations).toEqual([]);
    expect(result.parts[0].box).toEqual({ x0: 0, x1: 600, y0: 0, y1: 20, z0: 0, z1: 500 });
  });

  it("attaching a face on a panel's thickness axis replaces the opposite one", () => {
    const { assembly, id } = addPanel({ parts: [] }, "CARCASS", "Dessus");
    const moved = attach(partNamed(assembly, id)!, "TOP", { to: "CABINET", face: "TOP" });

    expect(moved.faces?.TOP).toEqual({ to: "CABINET", face: "TOP" });
    expect(moved.faces?.BOTTOM).toBeUndefined();
    const result = assemble({ parts: [moved] }, context);
    expect(result.parts[0].box.y0).toBe(680);
  });

  it("renaming a part follows it everywhere it is named", () => {
    let assembly = addPanel({ parts: [] }, "CARCASS", "Fond").assembly;
    const shelf = addPanel(assembly, "CARCASS", "Tablette");
    assembly = shelf.assembly;
    assembly = { ...assembly, parts: assembly.parts!.map(part => part.id === shelf.id
      ? attach(part, "BOTTOM", { to: "panel", face: "TOP" }) : part) };
    assembly = { ...addHardware(assembly), hardware: [{ material: { role: "SHELF_PIN" }, on: "panel", quantity: { fixed: 4 } }] };

    const renamed = rename(assembly, "panel", "bottom");

    expect(renamed.parts!.map(part => part.id)).toEqual(["bottom", "panel2"]);
    expect(renamed.parts![1].faces?.BOTTOM?.to).toBe("bottom");
    expect(renamed.hardware![0].on).toBe("bottom");
    expect(rename(renamed, "bottom", "panel2")).toBe(renamed);
  });

  it("removing a front stack removes the hardware counted on its fronts", () => {
    const stack = addStack({ parts: [] });
    const assembly = { ...stack.assembly, hardware: [{ material: { role: "HINGE" as const }, on: stack.id, quantity: { fixed: 2 } }] };

    expect(remove(assembly, stack.id)).toEqual({ ...assembly, frontStacks: [], hardware: [], parts: [], divider: undefined });
  });

  it("a divider is offered once and shares the names", () => {
    const withDivider = setDivider({ parts: [] }, true);
    expect(withDivider.divider?.id).toBe("divider");
    expect(uniqueName(withDivider, "divider")).toBe("divider2");
    expect(setDivider(withDivider, false).divider).toBeNull();
  });
});
