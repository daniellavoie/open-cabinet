import { Design } from "./model/design";

/** What a Design Proposal does to a Cabinet, Opening, Appliance or Filler. */
export type Mark = "ADDED" | "CHANGED" | "REMOVED";

type ListName = "cabinets" | "openings" | "appliances" | "fillers";

const LISTS: ListName[] = ["cabinets", "openings", "appliances", "fillers"];

/**
 * The Proposal's design to show beside the live one: everything it adds or
 * changes is marked, and what it removes is kept in view, marked removed,
 * when its Wall is still there.
 */
export function proposalMarks(live: Design, proposal: Design): { design: Design; marks: Map<string, Mark> } {
  const marks = new Map<string, Mark>();
  const design: Design = { ...proposal };
  const walls = new Set(proposal.walls.map(wall => wall.id));
  for (const list of LISTS) {
    const before = new Map((live[list] ?? []).map(item => [item.id, item] as const));
    const after = (proposal[list] ?? []) as { id: string; wallId: string }[];
    for (const item of after) {
      const old = before.get(item.id);
      if (!old) {
        marks.set(item.id, "ADDED");
      } else if (JSON.stringify(old) !== JSON.stringify(item)) {
        marks.set(item.id, "CHANGED");
      }
    }
    const kept = new Set(after.map(item => item.id));
    const removed = (live[list] ?? []).filter(item => !kept.has(item.id) && walls.has(item.wallId));
    removed.forEach(item => marks.set(item.id, "REMOVED"));
    (design as Record<ListName, unknown[]>)[list] = [...after, ...removed];
  }
  return { design, marks };
}

/** How many of each mark, for a short summary. */
export function countMarks(marks: Map<string, Mark>): Record<Mark, number> {
  const counts: Record<Mark, number> = { ADDED: 0, CHANGED: 0, REMOVED: 0 };
  marks.forEach(mark => counts[mark]++);
  return counts;
}
