# A Cabinet keeps the Template Version it was placed with

Cabinet Templates, the starter ones included, are published as numbered Template Versions that never change, and each Cabinet records the template and version it uses; it moves to another version only when a Member updates or reverts it, and versions in use are archived, never deleted. We chose this so a Project designed and quoted last year still produces the same Parts, Cut List and Bill of Materials after the Shop or Open Cabinet improves a template. Starter versions ship as files that are only ever added, with a test that fails when a published one is edited: fixing a starter template means publishing a new version, never correcting the old one.

## Considered Options

- **Always use the latest template** (the behaviour before this decision, where a Cabinet held only the template's code): simplest, but any template change silently alters every old Project.
- **Copy the whole template into each Cabinet**: old Projects are safe, but nothing links them to newer versions, so there is nothing to offer as an update or revert to.

## Consequences

- A Template Version captures the design only; material thicknesses, joinery, edge banding and gaps still follow the Shop's current Construction Standard.
- Updating a Cabinet keeps its own values and is refused when they break the new version's Construction Rules.
